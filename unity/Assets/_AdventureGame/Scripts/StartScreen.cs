using UnityEngine;
using UnityEngine.UI;

namespace AdventureGame
{
    /// <summary>
    /// Starting screen: title + PLAY + UPGRADES over a paused scene.
    /// Builds its own UI in code; added to the GameManager by the builder.
    /// Time stays frozen until Play (upgrades shoppable before the run).
    /// </summary>
    public class StartScreen : MonoBehaviour
    {
        GameObject panel;
        bool started;
        bool shopOpenedByMe;

        void Awake()
        {
            Time.timeScale = 0f;
            Build();
        }

        void Update()
        {
            if (started) return;
            Cursor.lockState = CursorLockMode.None;
            Cursor.visible = true;
            var ui = Object.FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
            bool shopOpen = ui != null && ui.IsShopOpen();
            // Shop closed -> come back to the title.
            if (shopOpenedByMe && !shopOpen)
            {
                shopOpenedByMe = false;
                if (panel != null) panel.SetActive(true);
            }
            // Enter starts the game ONLY when the shop is closed, so the key
            // can never hijack shopping.
            bool confirm = !shopOpen
                && UnityEngine.InputSystem.Keyboard.current != null
                && UnityEngine.InputSystem.Keyboard.current.enterKey.wasPressedThisFrame;
#if ENABLE_LEGACY_INPUT_MANAGER
            confirm |= !shopOpen && Input.GetKeyDown(KeyCode.Return);
#endif
            if (confirm) Play();
        }

        public bool HasStarted() => started;

        static void Deselect()
        {
            if (UnityEngine.EventSystems.EventSystem.current != null)
                UnityEngine.EventSystems.EventSystem.current.SetSelectedGameObject(null);
        }

        public void Play()
        {
            if (started) return;
            started = true;
            Deselect();
            if (panel != null) panel.SetActive(false);
            Time.timeScale = 1f;
        }

        void OpenUpgrades()
        {
            var ui = Object.FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
            if (ui == null) return;
            // The title dim layer would block + darken the shop: hide it first.
            shopOpenedByMe = true;
            if (panel != null) panel.SetActive(false);
            ui.ShowUpgrades(true);
            Deselect();
        }

        void Build()
        {
            var canvasGo = new GameObject("StartScreen", typeof(Canvas), typeof(GraphicRaycaster));
            canvasGo.transform.SetParent(transform, false);
            var canvas = canvasGo.GetComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            canvas.sortingOrder = 1000;
            panel = canvasGo;

            var bg = new GameObject("Dim", typeof(RectTransform), typeof(Image));
            bg.transform.SetParent(canvasGo.transform, false);
            Stretch(bg.GetComponent<RectTransform>());
            bg.GetComponent<Image>().color = new Color(0f, 0f, 0f, 0.78f);

            Label(canvasGo.transform, "Title", "MEADOW VALLEY", 54, new Vector2(0f, 120f), new Vector2(800f, 80f), TextAnchor.MiddleCenter, Color.white);
            Label(canvasGo.transform, "Subtitle", "Punch  •  Guard  •  Survive  •  Slay the Boss", 20, new Vector2(0f, 60f), new Vector2(800f, 36f), TextAnchor.MiddleCenter, new Color(1f, 0.85f, 0.3f));

            Button(canvasGo.transform, "PlayButton", "PLAY  (Enter)", new Vector2(0f, -30f), new Vector2(240f, 62f), new Color(0.2f, 0.65f, 0.25f), Play);
            Button(canvasGo.transform, "ShopButton", "UPGRADES", new Vector2(0f, -110f), new Vector2(240f, 52f), new Color(0.2f, 0.55f, 0.9f), OpenUpgrades);

            // Hint text matches the platform's controls.
            string controls = Application.isMobilePlatform
                ? "Joystick move  |  Drag right side: look  |  ATTACK  JUMP  RUN  GUARD  |  UP: shop"
                : "WASD move  |  Mouse look  |  LMB punch  |  RMB guard  |  Space jump  |  Shift sprint  |  U upgrades";
            Label(canvasGo.transform, "Controls", controls, 16,
                new Vector2(0f, -200f), new Vector2(900f, 30f), TextAnchor.MiddleCenter, new Color(0.8f, 0.8f, 0.8f));
        }

        static void Stretch(RectTransform rt)
        {
            rt.anchorMin = Vector2.zero; rt.anchorMax = Vector2.one;
            rt.offsetMin = Vector2.zero; rt.offsetMax = Vector2.zero;
        }

        static Text Label(Transform parent, string name, string text, int size, Vector2 pos, Vector2 dims, TextAnchor anchor, Color color)
        {
            var go = new GameObject(name, typeof(RectTransform), typeof(Text));
            go.transform.SetParent(parent, false);
            var rt = go.GetComponent<RectTransform>();
            rt.anchorMin = new Vector2(0.5f, 0.5f); rt.anchorMax = new Vector2(0.5f, 0.5f);
            rt.anchoredPosition = pos; rt.sizeDelta = dims;
            var t = go.GetComponent<Text>();
            t.text = text; t.fontSize = size; t.alignment = anchor;
            t.font = Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf")
                  ?? Resources.GetBuiltinResource<Font>("Arial.ttf");
            t.color = color;
            var outline = go.AddComponent<Outline>();
            outline.effectColor = Color.black;
            return t;
        }

        static void Button(Transform parent, string name, string text, Vector2 pos, Vector2 dims, Color color, UnityEngine.Events.UnityAction onClick)
        {
            var go = new GameObject(name, typeof(RectTransform), typeof(Image), typeof(UnityEngine.UI.Button));
            go.transform.SetParent(parent, false);
            var rt = go.GetComponent<RectTransform>();
            rt.anchorMin = new Vector2(0.5f, 0.5f); rt.anchorMax = new Vector2(0.5f, 0.5f);
            rt.anchoredPosition = pos; rt.sizeDelta = dims;
            go.GetComponent<Image>().color = color;
            var label = Label(go.transform, "Text", text, 20, Vector2.zero, dims, TextAnchor.MiddleCenter, Color.white);
            label.rectTransform.anchorMin = new Vector2(0.5f, 0.5f);
            label.rectTransform.anchorMax = new Vector2(0.5f, 0.5f);
            label.rectTransform.anchoredPosition = Vector2.zero;
            var btn = go.GetComponent<UnityEngine.UI.Button>();
            // No automatic keyboard navigation: selection must never jump
            // between title and shop buttons on its own.
            btn.navigation = new UnityEngine.UI.Navigation { mode = UnityEngine.UI.Navigation.Mode.None };
            btn.onClick.AddListener(onClick);
        }
    }
}
