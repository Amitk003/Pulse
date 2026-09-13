using UnityEngine;
using UnityEngine.InputSystem;

namespace AdventureGame
{
    /// <summary>
    /// Lightweight third-person orbit camera driven by the Player/Look action.
    /// No Cinemachine dependency. Handles collision so walls don't clip.
    /// </summary>
    public class AdventureThirdPersonCamera : MonoBehaviour
    {
        [Header("Target")]
        public Transform target;
        public Vector3 targetOffset = new Vector3(0f, 1.6f, 0f);

        [Header("Orbit")]
        public float distance = 4.2f;
        public float minDistance = 1.2f;
        public float sensitivity = 0.18f;
        public float topClamp = 70f;
        public float bottomClamp = -40f;
        public float followSmooth = 12f;

        public PlayerInput playerInput;

        float yaw;
        float pitch = 12f;
        Vector2 mobileLook;
        Vector3 smoothTarget;
        float shake;
        AdventureUI uiCache;
        StartScreen startCache;

        void Awake()
        {
            if (target == null)
            {
                var p = GameObject.FindGameObjectWithTag("Player");
                if (p != null) target = p.transform;
            }
            if (playerInput == null)
            {
                var p = target != null ? target.GetComponent<PlayerInput>() : null;
                if (p != null) playerInput = p;
                else playerInput = Object.FindAnyObjectByType<PlayerInput>(FindObjectsInactive.Exclude);
            }
            if (target != null)
            {
                smoothTarget = target.position + targetOffset;
                yaw = target.eulerAngles.y;
            }
            else yaw = transform.eulerAngles.y;
        }

        void OnEnable()
        {
            // Don't lock here: the title screen needs a free cursor, and the
            // first in-game click captures it via HandleCursorToggle.
        }

        void LateUpdate()
        {
            if (target == null) return;

            // Free cursor (Esc, menus) must not spin the camera on desktop:
            // only orbit while the pointer is locked. Touch look (mobile) always applies.
            Vector2 look = Application.isMobilePlatform || Cursor.lockState == CursorLockMode.Locked
                ? ReadLook()
                : GetMobileLookOnly();
            yaw += look.x * sensitivity;
            pitch -= look.y * sensitivity;
            pitch = Mathf.Clamp(pitch, bottomClamp, topClamp);

            smoothTarget = Vector3.Lerp(smoothTarget, target.position + targetOffset, Time.deltaTime * followSmooth);

            Quaternion rot = Quaternion.Euler(pitch, yaw, 0f);
            Vector3 desired = smoothTarget - rot * Vector3.forward * distance;

            // Camera collision: pull in when blocked.
            Vector3 dir = desired - smoothTarget;
            float dist = dir.magnitude;
            if (dist > 0.001f && Physics.SphereCast(smoothTarget, 0.3f, dir.normalized, out var hit, dist, ~0, QueryTriggerInteraction.Ignore))
            {
                if (!hit.transform.IsChildOf(target))
                    dist = Mathf.Max(minDistance, hit.distance - 0.2f);
            }
            dist = Mathf.Max(minDistance, Mathf.Min(distance, dist));

            transform.position = smoothTarget - rot * Vector3.forward * dist;
            transform.rotation = rot;

            if (shake > 0.001f)
            {
                transform.position += Random.insideUnitSphere * shake * 0.22f;
                shake = Mathf.Lerp(shake, 0f, Time.deltaTime * 8f);
            }
            else shake = 0f;

            HandleCursorToggle();
        }

        void HandleCursorToggle()
        {
            // Menus own the cursor: never lock while the title or shop is up,
            // or the very click that opens them gets hijacked into a game-lock.
            if (uiCache == null) uiCache = Object.FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
            if (startCache == null) startCache = Object.FindAnyObjectByType<StartScreen>(FindObjectsInactive.Exclude);
            bool shopOpen = uiCache != null && uiCache.IsShopOpen();
            bool onTitle = startCache != null && !startCache.HasStarted();
            if (onTitle || shopOpen)
            {
                Cursor.lockState = CursorLockMode.None;
                Cursor.visible = true;
                return;
            }
            bool escPressed = Keyboard.current != null && Keyboard.current.escapeKey.wasPressedThisFrame;
            bool clickPressed = Mouse.current != null && Mouse.current.leftButton.wasPressedThisFrame;
#if ENABLE_LEGACY_INPUT_MANAGER
            escPressed |= Input.GetKeyDown(KeyCode.Escape);
            clickPressed |= Input.GetMouseButtonDown(0);
#endif
            if (escPressed)
            {
                Cursor.lockState = CursorLockMode.None;
                Cursor.visible = true;
            }
            else if (Cursor.lockState == CursorLockMode.None && clickPressed)
            {
                Cursor.lockState = CursorLockMode.Locked;
                Cursor.visible = false;
            }
        }

        public void Shake(float amount)
        {
            shake = Mathf.Min(1f, shake + amount);
        }

        public void AddMobileLook(Vector2 delta)
        {
            mobileLook += delta;
        }

        Vector2 GetMobileLookOnly()
        {
            var touchLook = mobileLook;
            mobileLook = Vector2.zero;
            return touchLook;
        }

        Vector2 ReadLook()
        {
            var touchLook = mobileLook;
            mobileLook = Vector2.zero;
            if (playerInput != null && playerInput.actions != null)
            {
                var a = playerInput.actions.FindAction("Look", false);
                if (a != null) return a.ReadValue<Vector2>() + touchLook;
                var map = playerInput.actions.FindActionMap("Player");
                var l = map != null ? map.FindAction("Look") : null;
                if (l != null) return l.ReadValue<Vector2>() + touchLook;
            }
            // Fallback mouse delta scaled to behave like the action path.
#if ENABLE_LEGACY_INPUT_MANAGER
            return new Vector2(Input.GetAxis("Mouse X") * 8f, Input.GetAxis("Mouse Y") * 8f);
#else
            if (Mouse.current != null) return Mouse.current.delta.ReadValue() * 0.15f + touchLook;
            return touchLook;
#endif
        }
    }
}
