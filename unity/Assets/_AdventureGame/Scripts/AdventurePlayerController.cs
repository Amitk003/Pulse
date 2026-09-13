using UnityEngine;
using UnityEngine.InputSystem;

namespace AdventureGame
{
    /// <summary>
    /// New-Input-System third-person controller for the Statter humanoid.
    /// Drives the existing Statter Animator params (Y float, Jump / Jump End triggers)
    /// with explicit CharacterController movement (no root-motion required).
    /// Falls back to old Input Manager if no PlayerInput is present.
    /// </summary>
    [RequireComponent(typeof(CharacterController))]
    [RequireComponent(typeof(Animator))]
    public class AdventurePlayerController : MonoBehaviour
    {
        [Header("Refs (auto-wired by builder)")]
        public Transform cameraTransform;
        public PlayerInput playerInput;

        [Header("Move")]
        public float walkSpeed = 2.5f;
        public float runSpeed = 5.5f;
        public float rotateSmooth = 10f;
        public float moveBlendSmooth = 6f;

        [Header("Jump / Gravity")]
        public float jumpHeight = 1.5f;
        public float gravity = 20f;

        [Header("Fists")]
        public float punchRange = 2.1f;
        public int punchDamage = 12;
        public int finisherDamage = 20;
        [Tooltip("Provisional input-lock floors per combo step (jab / cross / finisher).")]
        public float jabCooldown = 0.34f;
        public float crossCooldown = 0.40f;
        public float finisherCooldown = 0.58f;
        [Tooltip("Fraction of the punch animation that must play before the next punch. 1 = full swing, 0.85 = cancel the last 15%.")]
        public float punchCancelFraction = 0.85f;
        [Tooltip("Clicks this close to the window opening are buffered (max 1) instead of dropped.")]
        public float bufferGrace = 0.14f;
        public float comboWindow = 1.0f;
        public float punchDriftSpeed = 3.2f;
        public float hitDelay = 0.16f;

        [Header("Sounds (auto-wired by builder, procedural fallback)")]
        public AudioClip whooshSound;
        public AudioClip hitSound;
        public AudioClip finisherSound;
        public AudioClip blockSound;
        public AudioClip landSound;

        [Header("Block (hold RMB)")]
        public float blockMoveMultiplier = 0.35f;
        public float staminaMax = 100f;
        public float staminaDrain = 30f;
        public float staminaRegen = 45f;
        public float blockedHitCost = 8f;
        public float guardBreakTime = 0.8f;

        [Header("Interact")]
        public float interactRadius = 3f;

        CharacterController cc;
        Animator animator;
        AudioSource sfx;
        AdventureThirdPersonCamera camShake;
        float blendY;
        float verticalVel;
        float attackTimer;
        bool bufferedAttack;
        bool jumpPressedLatch;
        int comboStep;
        float comboTimer;
        float punchDrift;
        Vector2 mobileMove;
        bool mobileSprint;
        bool mobileBlock;
        bool mobileJump;
        public static string lastPunch = "-";

        public bool IsBlocking { get; private set; }
        float stamina;
        float breakUntil;
        GameObject guardBubble;
        Material bubbleMat;
        float bubblePulse;
        float bubbleFlash;
        AdventureUI uiCache;
        StartScreen startCache;
        Camera fovCam;

        static AudioClip blockClip;

        static AudioClip whooshClip;
        static AudioClip thudClip;
        static Material puffMat;

        static readonly int HashY = Animator.StringToHash("Y");
        static readonly int HashJump = Animator.StringToHash("Jump");
        static readonly int HashJumpEnd = Animator.StringToHash("Jump End");
        static readonly int HashHit = Animator.StringToHash("Hit");
        static readonly int HashPunch1 = Animator.StringToHash("Punch1");
        static readonly int HashPunch2 = Animator.StringToHash("Punch2");
        static readonly int HashFinisher = Animator.StringToHash("PunchFinisher");

        void Awake()
        {
            cc = GetComponent<CharacterController>();
            animator = GetComponent<Animator>();
            if (playerInput == null) playerInput = GetComponent<PlayerInput>();
            if (cameraTransform == null && Camera.main != null) cameraTransform = Camera.main.transform;
            if (cameraTransform != null) camShake = cameraTransform.GetComponent<AdventureThirdPersonCamera>();
            // Explicit movement is more reliable than root motion for gameplay.
            animator.applyRootMotion = false;
            EnsureSfx();
            stamina = staminaMax;
        }

        void OnEnable()
        {
            // Poll-driven; also latch Jump/Attack/Interact performed events if PlayerInput exists.
            if (playerInput != null)
            {
                playerInput.onActionTriggered += OnActionTriggered;
            }
        }

        void OnDisable()
        {
            if (playerInput != null) playerInput.onActionTriggered -= OnActionTriggered;
        }

        void OnActionTriggered(InputAction.CallbackContext ctx)
        {
            if (!ctx.performed) return;
            if (ctx.action.name == "Jump") jumpPressedLatch = true;
            else if (ctx.action.name == "Attack") TryAttack();
            else if (ctx.action.name == "Interact") TryInteract();
        }

        void Update()
        {
            if (AdventureGameManager.Instance != null && AdventureGameManager.Instance.gameOver) return;
            // Title screen: world is frozen, inputs must not queue up behind it
            // (a punch pressed there would lock the combo timer until Play).
            if (startCache == null) startCache = Object.FindAnyObjectByType<StartScreen>(FindObjectsInactive.Exclude);
            if (startCache != null && !startCache.HasStarted()) return;

            Vector2 move;
            bool sprint;
            bool jumpPressed;
            ReadInput(out move, out sprint, out jumpPressed);

            if (jumpPressed) jumpPressedLatch = true;
            // Polled every frame: the event subscription in OnEnable is only a
            // backup now, so punches/interacts can never silently die.
            PollButtons();
            UpdateBlock();

            // Camera-relative move dir
            Vector3 camFwd = cameraTransform != null ? cameraTransform.forward : transform.forward;
            camFwd.y = 0f;
            if (camFwd.sqrMagnitude < 0.0001f) camFwd = transform.forward;
            camFwd.Normalize();
            // Right vector on the ground plane (forward rotated -90 deg around Y).
            Vector3 camRight = new Vector3(camFwd.z, 0f, -camFwd.x);

            Vector3 wishDir = (camFwd * move.y + camRight * move.x);
            if (wishDir.sqrMagnitude > 1f) wishDir.Normalize();
            bool moving = wishDir.sqrMagnitude > 0.0004f;

            float targetSpeed = sprint ? runSpeed : walkSpeed;
            if (IsBlocking)
            {
                targetSpeed *= blockMoveMultiplier;
                sprint = false;
            }
            float targetY = !moving ? 0f : (sprint ? 1f : 0.5f);
            blendY = Mathf.Lerp(blendY, targetY, Time.deltaTime * moveBlendSmooth);
            animator.SetFloat(HashY, blendY);

            if (moving)
            {
                Quaternion look = Quaternion.LookRotation(wishDir, Vector3.up);
                transform.rotation = Quaternion.Slerp(transform.rotation, look, Time.deltaTime * rotateSmooth);
            }

            // Gravity + jump
            bool grounded = cc.isGrounded;
            if (grounded && verticalVel < 0f) verticalVel = -2f;
            if (jumpPressedLatch)
            {
                jumpPressedLatch = false;
                if (grounded)
                {
                    verticalVel = Mathf.Sqrt(2f * gravity * jumpHeight);
                    animator.SetTrigger(HashJump);
                }
            }
            verticalVel -= gravity * Time.deltaTime;

            Vector3 motion = wishDir * (moving ? targetSpeed : 0f) * Time.deltaTime;
            motion.y = verticalVel * Time.deltaTime;
            // Forward drift during a punch swing (replaces the old instant lunge).
            if (punchDrift > 0.01f)
            {
                Vector3 pf = transform.forward;
                pf.y = 0f;
                if (pf.sqrMagnitude > 0.001f) motion += pf.normalized * punchDrift * Time.deltaTime;
                punchDrift = Mathf.Lerp(punchDrift, 0f, Time.deltaTime * 7f);
            }
            cc.Move(motion);

            if (!grounded && cc.isGrounded)
            {
                animator.SetTrigger(HashJumpEnd);
                if (verticalVel < -9f) OnLanded(-verticalVel);
            }

            // Sprint FOV kick for speed feel.
            if (fovCam == null) fovCam = Camera.main;
            if (fovCam != null)
            {
                float targetFov = (sprint && moving && !IsBlocking) ? 66f : 60f;
                fovCam.fieldOfView = Mathf.Lerp(fovCam.fieldOfView, targetFov, Time.deltaTime * 4f);
            }

            attackTimer -= Time.deltaTime;
            if (comboTimer > 0f)
            {
                comboTimer -= Time.deltaTime;
                if (comboTimer <= 0f) comboStep = 0;
            }
            // A buffered click fires the moment the current swing recovers
            // (unless guard went up in the meantime).
            if (bufferedAttack && attackTimer <= 0f)
            {
                bufferedAttack = false;
                if (!IsBlocking) DoPunch();
            }

#if ENABLE_LEGACY_INPUT_MANAGER
            // Old-input fallback for attack/interact keys (also works alongside new system)
            if (playerInput == null)
            {
                if (Input.GetMouseButtonDown(0)) TryAttack();
                if (Input.GetKeyDown(KeyCode.E)) TryInteract();
            }
#endif
        }

        void PollButtons()
        {
            if (playerInput != null && playerInput.actions != null)
            {
                var map = playerInput.actions.FindActionMap("Player");
                if (map != null)
                {
                    var a = map.FindAction("Attack");
                    if (a != null && a.WasPressedThisFrame()) TryAttack();
                    var ie = map.FindAction("Interact");
                    if (ie != null && ie.WasPressedThisFrame()) TryInteract();
                }
            }
        }

        public void SetMobileMove(Vector2 value)
        {
            mobileMove = Vector2.ClampMagnitude(value, 1f);
        }

        public void SetMobileSprint(bool value)
        {
            mobileSprint = value;
        }

        public void SetMobileBlock(bool value)
        {
            mobileBlock = value;
        }

        public bool MobileAttack()
        {
            TryAttack();
            return true;
        }

        /// <summary>Called by MobileControls: UP toggles the upgrade shop mid-run.</summary>
        public void MobileToggleUpgrades()
        {
            if (uiCache == null) uiCache = Object.FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
            if (uiCache != null) uiCache.ToggleUpgrades();
        }

        public bool MobileJump()
        {
            mobileJump = true;
            return true;
        }

        bool BlockHeld()
        {
            bool held = Mouse.current != null && Mouse.current.rightButton.isPressed;
#if ENABLE_LEGACY_INPUT_MANAGER
            held |= Input.GetMouseButton(1);
#endif
            var pad = Gamepad.current;
            if (pad != null) held |= pad.leftTrigger.ReadValue() > 0.3f;
            held |= mobileBlock;
            return held;
        }

        void UpdateBlock()
        {
            bool wantBlock = BlockHeld() && Time.time >= breakUntil && stamina > 0f;
            if (wantBlock && !IsBlocking)
            {
                IsBlocking = true;
                bubbleFlash = 1f;
                if (guardBubble == null) guardBubble = BuildGuardBubble();
                guardBubble.SetActive(true);
            }
            else if (!wantBlock && IsBlocking)
            {
                IsBlocking = false;
                if (guardBubble != null) guardBubble.SetActive(false);
            }

            if (IsBlocking)
            {
                stamina = Mathf.Max(0f, stamina - staminaDrain * Time.deltaTime);
                // Ghost guard: nearly invisible idle, flashes on raise + hits.
                bubbleFlash = Mathf.Max(0f, bubbleFlash - Time.deltaTime * 3f);
                if (bubbleMat != null)
                {
                    bubblePulse += Time.deltaTime * 3f;
                    float a = 0.015f + 0.11f * bubbleFlash
                        + 0.006f * (0.5f + 0.5f * Mathf.Sin(bubblePulse));
                    var bc = bubbleMat.HasProperty("_BaseColor")
                        ? bubbleMat.GetColor("_BaseColor") : Color.white;
                    bc.a = a;
                    if (bubbleMat.HasProperty("_BaseColor")) bubbleMat.SetColor("_BaseColor", bc);
                    else if (bubbleMat.HasProperty("_Color")) bubbleMat.color = bc;
                }
                if (stamina <= 0f)
                {
                    IsBlocking = false;
                    breakUntil = Time.time + guardBreakTime;
                    if (guardBubble != null) guardBubble.SetActive(false);
                    if (uiCache == null) uiCache = Object.FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
                    if (uiCache != null) uiCache.ShowMessage("GUARD BREAK!", 1.2f);
                }
            }
            else
            {
                stamina = Mathf.Min(staminaMax, stamina + staminaRegen * Time.deltaTime);
            }
            if (uiCache == null) uiCache = Object.FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
            if (uiCache != null) uiCache.SetStamina(stamina / staminaMax, IsBlocking);
        }

        GameObject BuildGuardBubble()
        {
            // Proper heater-shield silhouette, held left-front at chest height.
            var go = new GameObject("GuardShield", typeof(MeshFilter), typeof(MeshRenderer));
            go.GetComponent<MeshFilter>().sharedMesh = GuardShieldMesh.CreateHeaterShield();
            go.transform.SetParent(transform, false);
            go.transform.localPosition = new Vector3(-0.22f, 1.18f, 0.42f);
            go.transform.localRotation = Quaternion.Euler(0f, -12f, 0f);
            go.transform.localScale = Vector3.one * 0.9f;
            go.GetComponent<Renderer>().shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            var shader = Shader.Find("Universal Render Pipeline/Unlit") ?? Shader.Find("Sprites/Default");
            var mat = new Material(shader);
            var tint = new Color(0.92f, 0.96f, 1f, 0.02f);
            if (mat.HasProperty("_BaseColor")) mat.SetColor("_BaseColor", tint);
            else if (mat.HasProperty("_Color")) mat.color = tint;
            if (mat.HasProperty("_Cull")) mat.SetFloat("_Cull", 0f);
            if (mat.HasProperty("_Surface"))
            {
                mat.SetFloat("_Surface", 1f);
                mat.SetFloat("_Blend", 0f);
                mat.SetOverrideTag("RenderType", "Transparent");
                mat.renderQueue = 3000;
            }
            go.GetComponent<Renderer>().sharedMaterial = mat;
            bubbleMat = mat;
            go.SetActive(false);
            return go;
        }

        /// <summary>Called by the game manager instead of taking damage.</summary>
        public void OnBlockedHit(int amount)
        {
            stamina = Mathf.Max(0f, stamina - blockedHitCost);
            bubbleFlash = 1f;
            if (blockClip == null) blockClip = MakePunchClip(0.1f, 2200f, 900f, 0.45f, 4242);
            var bclip = blockSound != null ? blockSound : blockClip;
            if (sfx != null && bclip != null) sfx.PlayOneShot(bclip);
            if (camShake != null) camShake.Shake(0.15f);
            if (uiCache == null) uiCache = Object.FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
            if (uiCache != null) uiCache.SetStamina(stamina / staminaMax, IsBlocking);
        }

        void ReadInput(out Vector2 move, out bool sprint, out bool jump)
        {
            move = Vector2.zero;
            sprint = false;
            jump = false;
            if (playerInput != null && playerInput.actions != null)
            {
                var map = playerInput.actions.FindActionMap("Player");
                if (map != null)
                {
                    var m = map.FindAction("Move");
                    var s = map.FindAction("Sprint");
                    var j = map.FindAction("Jump");
                    if (m != null) move = m.ReadValue<Vector2>();
                    if (s != null) sprint = s.IsPressed();
                    if (j != null) jump = j.WasPressedThisFrame();
                    move = mobileMove.sqrMagnitude > 0.0001f ? mobileMove : move;
                    sprint |= mobileSprint;
                    jump |= mobileJump;
                    mobileJump = false;
                    return;
                }
            }
            // Fallback: old Input Manager (only compiled when enabled; keeps Demo playable)
#if ENABLE_LEGACY_INPUT_MANAGER
            move = new Vector2(Input.GetAxis("Horizontal"), Input.GetAxis("Vertical"));
            sprint = Input.GetKey(KeyCode.LeftShift);
            jump = Input.GetKeyDown(KeyCode.Space);
#else
            move = Vector2.zero;
#endif
            move = mobileMove.sqrMagnitude > 0.0001f ? mobileMove : move;
            sprint |= mobileSprint;
            jump |= mobileJump;
            mobileJump = false;
        }

        public void TryAttack()
        {
            if (IsBlocking) return; // no punching while guarding
            if (AdventureGameManager.Instance != null && AdventureGameManager.Instance.gameOver) return;

            // Anti-spam pacing: each combo step must play out before the next.
            if (attackTimer > 0f)
            {
                // Only a click just before the window opens is kept (max one),
                // so mashing can never queue up a burst of swings.
                if (attackTimer <= bufferGrace) bufferedAttack = true;
                return;
            }
            DoPunch();
        }

        void DoPunch()
        {
            bufferedAttack = false;

            // 3-hit combo: jab, cross, finisher. Missing the window restarts it.
            comboStep = comboTimer > 0f ? comboStep + 1 : 1;
            if (comboStep > 3) comboStep = 1;
            comboTimer = comboWindow;
            bool finisher = comboStep == 3;
            int visualStep = comboStep;   // 1 jab, 2 cross (finisher resets step)
            if (finisher) comboStep = 0;  // combo ends on the finisher

            // Provisional floor; extended to the real swing length below so
            // every punch PLAYS OUT — no more chopped, strobing half-swings.
            attackTimer = finisher ? finisherCooldown : (visualStep == 1 ? jabCooldown : crossCooldown);
            StartCoroutine(SyncLockToSwing(visualStep, finisher));

            // Play the real punch animation (wired from your Punching FBX files);
            // damage lands mid-swing via the delayed hit below.
            animator.SetTrigger(finisher ? HashFinisher : (visualStep == 1 ? HashPunch1 : HashPunch2));
            punchDrift = punchDriftSpeed * (finisher ? 1.4f : 1f);
            PlayWhoosh();
            // Fires every swing, enemies or not: swing flash + HUD proof.
            lastPunch = (finisher ? "FINISHER" : (visualStep == 1 ? "JAB" : "CROSS")) + " @ " + Time.time.ToString("F1") + "s";
            Vector3 swing = transform.position + Vector3.up * 1.2f + transform.forward * 1.1f;
            SpawnPunchPuff(swing, false);
            StartCoroutine(PunchHitDelayed(finisher, hitDelay));
        }

        /// <summary>
        /// Extends attackTimer to match the actual punch clip length, so the
        /// next input can only start once this swing has (nearly) finished.
        /// </summary>
        System.Collections.IEnumerator SyncLockToSwing(int visualStep, bool finisher)
        {
            int targetHash = finisher ? HashFinisher : (visualStep == 1 ? HashPunch1 : HashPunch2);
            // Transition may take a few frames; poll until the punch state plays.
            for (int i = 0; i < 20; i++)
            {
                yield return null;
                if (animator == null) yield break;
                if (animator.GetCurrentAnimatorStateInfo(0).shortNameHash == targetHash) break;
            }
            if (animator == null) yield break;
            var info = animator.GetCurrentAnimatorStateInfo(0);
            if (info.shortNameHash != targetHash) yield break; // trigger eaten; floor stands
            // length is the state's real duration (already includes the 1.35x/
            // 1.1x state speeds), so lock = most of the actual swing.
            float lockTime = info.length * Mathf.Clamp(punchCancelFraction, 0.5f, 1f);
            if (lockTime > attackTimer) attackTimer = lockTime;
        }

        System.Collections.IEnumerator PunchHitDelayed(bool finisher, float delay)
        {
            yield return new WaitForSeconds(delay);
            if (AdventureGameManager.Instance != null && AdventureGameManager.Instance.gameOver) yield break;
            Vector3 center = transform.position + Vector3.up * 1.2f + transform.forward * 1.1f;
            var hits = Physics.OverlapSphere(center, punchRange, ~0, QueryTriggerInteraction.Ignore);
            foreach (var h in hits)
            {
                var enemy = h.GetComponentInParent<EnemyChaser>();
                if (enemy == null) continue;
                int dmg = Mathf.RoundToInt((finisher ? finisherDamage : punchDamage)
                    * (AdventureGameManager.Instance != null ? AdventureGameManager.Instance.AttackMultiplier() : 1f));
                Vector3 from = transform.position;
                enemy.TakeDamage(dmg, from);
                PlayThud(finisher);
                Vector3 at = h.ClosestPoint(center);
                SpawnPunchPuff(at, finisher);
                // Red blood for the boss, sparks for everything else.
                bool isBoss = enemy is BossEnemy;
                Color burst = isBoss
                    ? new Color(0.85f, 0.05f, 0.08f)
                    : finisher ? new Color(1f, 0.55f, 0.15f) : Color.white;
                HitBurst.Spawn(at, burst, (finisher ? 1.6f : 1f) * (isBoss ? 1.4f : 1f), isBoss ? 10 : 6);
                if (finisher) HitBurst.Shockwave(transform.position,
                    isBoss ? new Color(0.85f, 0.05f, 0.08f) : new Color(1f, 0.6f, 0.2f), 2.5f);
                if (camShake != null) camShake.Shake(finisher ? 0.7f : 0.35f);
                HitStop(finisher ? 0.09f : 0.045f, finisher ? 0.05f : 0.2f);
            }
        }

        // -1 = not in hit-stop. Kept across overlapping stops so the ORIGINAL
        // time scale is always restored (e.g. 0 while the title screen is up).
        static float timeScaleBeforeHitStop = -1f;

        void HitStop(float duration, float scale)
        {
            StopCoroutine("HitStopRoutine");
            StartCoroutine(HitStopRoutine(duration, scale));
        }

        System.Collections.IEnumerator HitStopRoutine(float duration, float scale)
        {
            if (timeScaleBeforeHitStop < 0f) timeScaleBeforeHitStop = Time.timeScale;
            Time.timeScale = scale;
            yield return new WaitForSecondsRealtime(duration);
            Time.timeScale = timeScaleBeforeHitStop;
            timeScaleBeforeHitStop = -1f;
        }

        void SpawnPunchPuff(Vector3 pos, bool big)
        {
            var go = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            go.name = "PunchPuff";
            var col = go.GetComponent<SphereCollider>();
            if (col != null) Destroy(col);
            go.transform.position = pos;
            go.transform.localScale = Vector3.one * (big ? 0.3f : 0.18f);
            if (puffMat == null)
            {
                var shader = Shader.Find("Universal Render Pipeline/Unlit") ?? Shader.Find("Sprites/Default");
                puffMat = new Material(shader);
                if (puffMat.HasProperty("_BaseColor")) puffMat.SetColor("_BaseColor", Color.white);
                else if (puffMat.HasProperty("_Color")) puffMat.color = Color.white;
            }
            go.GetComponent<Renderer>().sharedMaterial = puffMat;
            Destroy(go, 0.09f);
        }

        void OnLanded(float impact)
        {
            Vector3 feet = transform.position + Vector3.up * 0.15f;
            HitBurst.Spawn(feet, new Color(0.7f, 0.65f, 0.55f), 0.8f, 5);
            if (sfx != null) sfx.PlayOneShot(landSound != null ? landSound : thudClip, 0.7f);
        }

        void EnsureSfx()
        {
            if (sfx == null)
            {
                sfx = gameObject.AddComponent<AudioSource>();
                sfx.playOnAwake = false;
                sfx.spatialBlend = 0.3f;
            }
            if (whooshClip == null) whooshClip = MakePunchClip(0.12f, 1200f, 300f, 0.5f, 1234);
            if (thudClip == null) thudClip = MakePunchClip(0.18f, 170f, 60f, 0.8f, 777);
        }

        static AudioClip MakePunchClip(float seconds, float freqFrom, float freqTo, float volume, int seed)
        {
            int rate = 22050;
            int n = Mathf.Max(1, (int)(rate * seconds));
            var data = new float[n];
            var rand = new System.Random(seed);
            float phase = 0f;
            for (int i = 0; i < n; i++)
            {
                float t = (float)i / n;
                phase += 2f * Mathf.PI * Mathf.Lerp(freqFrom, freqTo, t) / rate;
                float env = (1f - t) * (1f - t);
                data[i] = (Mathf.Sin(phase) * 0.6f + ((float)rand.NextDouble() * 2f - 1f) * 0.4f) * env * volume;
            }
            var clip = AudioClip.Create("PunchSfx", n, 1, rate, false);
            clip.SetData(data, 0);
            return clip;
        }

        void PlayWhoosh()
        {
            var clip = whooshSound != null ? whooshSound : whooshClip;
            if (sfx != null && clip != null) sfx.PlayOneShot(clip);
        }

        void PlayThud(bool heavy)
        {
            var clip = heavy
                ? (finisherSound != null ? finisherSound : thudClip)
                : (hitSound != null ? hitSound : thudClip);
            if (sfx != null && clip != null) sfx.PlayOneShot(clip);
        }

        public void TryInteract()
        {
            var npcs = Object.FindObjectsByType<FriendlyNPC>(FindObjectsInactive.Exclude);
            FriendlyNPC best = null;
            float bestD = interactRadius;
            foreach (var n in npcs)
            {
                float d = Vector3.Distance(transform.position, n.transform.position);
                if (d <= interactRadius && (best == null || d < bestD)) { best = n; bestD = d; }
            }
            if (best != null) best.Interact();
        }

        public void PlayHit()
        {
            if (animator != null) animator.SetTrigger(HashHit);
        }

        public void ResetVerticalVelocity()
        {
            verticalVel = 0f;
            blendY = 0f;
        }

        void OnDrawGizmosSelected()
        {
            Gizmos.color = Color.red;
            Vector3 c = transform.position + Vector3.up * 1.2f + transform.forward * 1.2f;
            Gizmos.DrawWireSphere(c, punchRange);
        }
    }
}
