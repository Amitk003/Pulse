using UnityEngine;
using UnityEngine.InputSystem;
using UnityEngine.UI;

namespace AdventureGame
{
    /// <summary>
    /// On-screen controls cheat-sheet + live input readout, rendered as UGUI
    /// (OnGUI ran several times per frame and cost real CPU/GC).
    /// Text refreshes 8x/sec. Toggle with F1.
    /// Added automatically to the GameManager by the level builder.
    /// </summary>
    public class ControlsDebug : MonoBehaviour
    {
        public bool visible = true;
        public PlayerInput playerInput;

        Vector2 move;
        Vector2 look;
        bool sprint;
        string lastAction = "none";
        Text overlayText;
        float nextRefresh;

        void Awake()
        {
            // Keyboard/mouse cheat-sheet is noise on touch screens.
            if (Application.isMobilePlatform)
            {
                enabled = false;
                return;
            }
            if (playerInput == null)
                playerInput = Object.FindAnyObjectByType<PlayerInput>(FindObjectsInactive.Exclude);
            BuildOverlay();
        }

        void Update()
        {
            if (playerInput == null)
            {
                playerInput = Object.FindAnyObjectByType<PlayerInput>(FindObjectsInactive.Exclude);
                if (playerInput == null) return;
            }
            var map = playerInput.actions != null ? playerInput.actions.FindActionMap("Player") : null;
            if (map == null) return;
            var m = map.FindAction("Move");
            if (m != null) move = m.ReadValue<Vector2>();
            var l = map.FindAction("Look");
            if (l != null) look = l.ReadValue<Vector2>();
            var s = map.FindAction("Sprint");
            if (s != null) sprint = s.IsPressed();
            foreach (var a in map.actions)
            {
                if (a.WasPressedThisFrame()) lastAction = $"{a.name} @ {Time.time:F1}s";
            }

            if (Keyboard.current != null && Keyboard.current.f1Key.wasPressedThisFrame)
            {
                visible = !visible;
                if (overlayText != null) overlayText.gameObject.SetActive(visible);
            }

            if (visible && overlayText != null && Time.unscaledTime >= nextRefresh)
            {
                nextRefresh = Time.unscaledTime + 0.125f;
                var mgr = AdventureGameManager.Instance;
                overlayText.text =
                    "Controls (F1 hides)\n" +
                    "WASD / arrows: move  |  Mouse: look\n" +
                    "Space: jump  |  Shift: sprint  |  R: restart\n" +
                    "Left-click: punch combo  |  Hold RMB: guard  |  U: upgrades\n" +
                    $"Move: {move}   Sprint: {sprint}\n" +
                    $"Look: {look}\n" +
                    $"Last button: {lastAction}\n" +
                    $"Punch: {AdventurePlayerController.lastPunch}\n" +
                    (mgr != null ? $"Shards {mgr.collected}/{mgr.totalCollectibles}" : "");
            }
        }

        void BuildOverlay()
        {
            var canvas = Object.FindAnyObjectByType<Canvas>(FindObjectsInactive.Exclude);
            Transform parent;
            if (canvas != null) parent = canvas.transform;
            else
            {
                var go = new GameObject("DebugCanvas", typeof(RectTransform), typeof(Canvas));
                var c = go.GetComponent<Canvas>();
                c.renderMode = RenderMode.ScreenSpaceOverlay;
                c.sortingOrder = 999;
                parent = go.transform;
            }
            var label = new GameObject("ControlsDebug", typeof(RectTransform), typeof(Text));
            label.transform.SetParent(parent, false);
            var rt = label.GetComponent<RectTransform>();
            rt.anchorMin = new Vector2(0f, 1f); rt.anchorMax = new Vector2(0f, 1f);
            rt.anchoredPosition = new Vector2(190f, -230f); rt.sizeDelta = new Vector2(360f, 280f);
            overlayText = label.GetComponent<Text>();
            overlayText.fontSize = 14;
            overlayText.font = Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf")
                  ?? Resources.GetBuiltinResource<Font>("Arial.ttf");
            overlayText.color = Color.white;
            var outline = label.AddComponent<Outline>();
            outline.effectColor = Color.black;
            label.SetActive(visible);
        }
    }
}
