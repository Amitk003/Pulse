using UnityEngine;

namespace AdventureGame
{
    /// <summary>
    /// Monster01 boss: heavy HP, runs the player down, cycles 3 animated
    /// melee attacks (Attack03 is a slam AoE), flinches on hits, dies with
    /// the Die clip and wins the game for the player ("BOSS SLAIN!").
    /// Driven by state NAME (their controller has no parameters).
    /// Attach to the Monster01 prefab instance next to its Animator.
    /// </summary>
    public class BossEnemy : EnemyChaser
    {
        [Header("Boss")]
        public string bossName = "MONSTER-01";        public float slamRange = 3.4f;
        public int slamDamage = 25;
        public int[] attackDamages = { 15, 18, 25 };
        public float attackHitDelay = 0.45f;
        public float slamHitDelay = 0.6f;
        public float getHitCooldown = 1.0f;

        [Header("Sounds (auto-wired by builder)")]
        public AudioClip slamSound;

        static readonly string[] AttackAnims = { "Monster01_Attack01", "Monster01_Attack02", "Monster01_Attack03" };

        Animator anim;
        AdventureUI ui;
        AudioSource sfx;
        string currentAnim = "";
        bool dead;
        bool aggroed;
        float lastGetHit = -99f;
        float attackLockUntil;
        int attackCycle;
        int bossPatrol;
        Vector3 baseScale;

        protected override void Awake()
        {
            maxHealth = 240;
            chaseRange = 12f;
            chaseSpeed = 3.2f;
            patrolSpeed = 1.0f;
            attackRange = 2.8f;
            attackCooldown = 1.9f;
            knockbackPower = 1.2f; // heavy: barely shoved
            base.Awake();
            // Animator may sit on the model node (clip paths are relative to it).
            anim = GetComponentInChildren<Animator>();
            sfx = gameObject.AddComponent<AudioSource>();
            sfx.playOnAwake = false;
            sfx.spatialBlend = 0.6f;
        }

        void Start()
        {
            if (anim == null) anim = GetComponentInChildren<Animator>();
            var ctrl = anim != null ? anim.runtimeAnimatorController : null;
            Debug.Log($"[Adventure] Boss anim check: animator={(anim != null ? anim.name : "NULL")}, " +
                      $"controller={(ctrl != null ? ctrl.name : "NULL")}, " +
                      $"states={(ctrl != null ? ctrl.animationClips.Length : 0)} clips");
        }

        protected override void Update()
        {
            var mgr = AdventureGameManager.Instance;
            if (mgr != null && mgr.gameOver) return;
            if (player == null)
            {
                var p = GameObject.FindGameObjectWithTag("Player");
                if (p != null) player = p.transform;
            }
            if (dead)
            {
                if (cc.isGrounded && verticalVel < 0f) verticalVel = -2f;
                verticalVel -= gravity * Time.deltaTime;
                cc.Move(new Vector3(0f, verticalVel, 0f) * Time.deltaTime);
                return;
            }

            attackTimer -= Time.deltaTime;
            Vector3 move = Vector3.zero;
            // While an attack/gethit is playing, locomotion must NOT override it.
            bool acting = Time.time < attackLockUntil;

            if (player != null)
            {
                float d = Vector3.Distance(transform.position, player.position);
                if (!aggroed && d <= chaseRange)
                {
                    aggroed = true;
                    if (ui == null) ui = Object.FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
                    if (ui != null) ui.ShowBossHP(bossName, health, maxHealth);
                    if (ui != null) ui.ShowMessage($"{bossName} has seen you!", 2.5f);
                    var aggroCam = Camera.main != null ? Camera.main.GetComponent<AdventureThirdPersonCamera>() : null;
                    if (aggroCam != null) aggroCam.Shake(0.5f);
                }
                if (aggroed)
                {
                    Vector3 to = player.position - transform.position;
                    to.y = 0f;
                    if (to.sqrMagnitude > 0.01f)
                        transform.rotation = Quaternion.Slerp(transform.rotation,
                            Quaternion.LookRotation(to.normalized), Time.deltaTime * 6f);
                    if (!acting)
                    {
                        if (d > attackRange)
                        {
                            move = to.normalized * chaseSpeed;
                            PlayAnim("Monster01_Run");
                        }
                        else if (attackTimer <= 0f)
                        {
                            StartAttack();
                        }
                        else PlayAnim("Monster01_Idle");
                    }
                }
            }

            if (!aggroed && !acting)
            {
                Vector3 dest = home + patrolOffsets[bossPatrol % patrolOffsets.Length];
                dest.y = transform.position.y;
                Vector3 to = dest - transform.position;
                to.y = 0f;
                if (to.magnitude < 0.6f) bossPatrol++;
                else
                {
                    transform.rotation = Quaternion.Slerp(transform.rotation,
                        Quaternion.LookRotation(to.normalized), Time.deltaTime * 4f);
                    move = to.normalized * patrolSpeed;
                    PlayAnim("Monster01_Walk");
                }
            }

            if (cc.isGrounded && verticalVel < 0f) verticalVel = -2f;
            verticalVel -= gravity * Time.deltaTime;
            move += knockback;
            knockback = Vector3.Lerp(knockback, Vector3.zero, Time.deltaTime * 8f);
            move.y = verticalVel;
            cc.Move(move * Time.deltaTime);

            if (flinch > 0f) flinch = Mathf.Max(0f, flinch - Time.deltaTime * 6f);
            // Recoil snap, relative to the fitted base scale (never resets it).
            if (baseScale == Vector3.zero) baseScale = transform.localScale;
            float f = flinch;
            transform.localScale = new Vector3(
                baseScale.x * (1f - 0.12f * f),
                baseScale.y * (1f + 0.18f * f),
                baseScale.z * (1f - 0.12f * f));
        }

        void StartAttack()
        {
            attackTimer = attackCooldown;
            int index = attackCycle % AttackAnims.Length;
            attackCycle++;
            // Smooth restart: short blend instead of a snap.
            currentAnim = AttackAnims[index];
            if (anim != null) anim.CrossFadeInFixedTime(currentAnim, 0.12f);
            else Debug.LogWarning("[Adventure] Boss attack with no Animator!");
            attackLockUntil = Time.time + 1.0f; // swing plays out fully, uninterruptable
            StartCoroutine(BossHitDelayed(index));
        }

        System.Collections.IEnumerator BossHitDelayed(int index)
        {
            yield return new WaitForSeconds(index == 2 ? slamHitDelay : attackHitDelay);
            if (dead) yield break;
            var mgr = AdventureGameManager.Instance;
            if (mgr == null || mgr.gameOver || player == null) yield break;
            float range = index == 2 ? slamRange : attackRange;
            if (Vector3.Distance(transform.position, player.position) <= range + 0.6f)
            {
                mgr.DamagePlayer(attackDamages[Mathf.Clamp(index, 0, attackDamages.Length - 1)]);
                if (index == 2)
                {
                    var cam = Camera.main != null ? Camera.main.GetComponent<AdventureThirdPersonCamera>() : null;
                    if (cam != null) cam.Shake(0.8f);
                    HitBurst.Shockwave(transform.position, new Color(1f, 0.4f, 0.1f), 4f);
                    if (sfx != null && slamSound != null) sfx.PlayOneShot(slamSound);
                }
            }
        }

        public override void TakeDamage(int amount, Vector3 hitFrom = default)
        {
            if (dead) return;
            base.TakeDamage(amount, hitFrom);
            if (ui == null) ui = Object.FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
            if (ui != null) ui.ShowBossHP(bossName, health, maxHealth);
            if (dead) return; // base.Die ran
            if (Time.time >= attackLockUntil && Time.time - lastGetHit >= getHitCooldown)
            {
                lastGetHit = Time.time;
                PlayAnim("Monster01_GetHit", 0.1f, true);
                attackLockUntil = Mathf.Max(attackLockUntil, Time.time + 0.45f);
            }
        }

        protected override void OnTookHit()
        {
            // No tint flash: it would permanently bleach the cel-shaded
            // materials. GetHit animation + knockback already sell the hit.
        }

        protected override void Die()
        {
            if (dead) return;
            dead = true;
            PlayAnim("Monster01_Die", 0.15f, true);
            if (ui != null) ui.HideBoss();
            var mgr = AdventureGameManager.Instance;
            if (mgr != null) mgr.StartCoroutine(BossDeathRoutine(mgr));
            else Destroy(gameObject, 2.5f);
        }

        System.Collections.IEnumerator BossDeathRoutine(AdventureGameManager mgr)
        {
            yield return new WaitForSeconds(2.2f);
            Destroy(gameObject);
            mgr.OnBossDefeated();
        }

        void PlayAnim(string animName, float fade = 0.2f, bool force = false)
        {
            if (anim == null) return;
            if (!force && currentAnim == animName) return;
            currentAnim = animName;
            anim.CrossFadeInFixedTime(animName, fade);
        }
    }
}
