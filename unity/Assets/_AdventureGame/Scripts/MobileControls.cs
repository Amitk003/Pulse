using System;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.InputSystem.UI;
using UnityEngine.UI;

namespace AdventureGame
{
    /// <summary>
    /// Builds the touch controls needed by the Android arena build.
    /// Desktop input remains available in the Unity Editor.
    /// </summary>
    public sealed class MobileControls : MonoBehaviour
    {
        Canvas canvas;
        MobileJoystick joystick;
        MobileLookArea lookArea;
        AdventurePlayerController player;
        AdventureThirdPersonCamera cameraRig;
        StartScreen startScreen;
        readonly System.Collections.Generic.List<GameObject> gameplayControls = new();
        GameObject restartControl;

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        static void Bootstrap()
        {
            if (!Application.isMobilePlatform) return;
            if (FindAnyObjectByType<MobileControls>() != null) return;
            new GameObject("MobileControls").AddComponent<MobileControls>();
        }

        void Awake()
        {
            DontDestroyOnLoad(gameObject);
            player = FindAnyObjectByType<AdventurePlayerController>();
            cameraRig = FindAnyObjectByType<AdventureThirdPersonCamera>();
            startScreen = FindAnyObjectByType<StartScreen>();
            BuildCanvas();
        }

        void Update()
        {
            if (player == null) player = FindAnyObjectByType<AdventurePlayerController>();
            if (cameraRig == null) cameraRig = FindAnyObjectByType<AdventureThirdPersonCamera>();
            if (startScreen == null) startScreen = FindAnyObjectByType<StartScreen>();
            if (player == null) return;

            bool gameStarted = startScreen == null || startScreen.HasStarted();
            if (canvas != null && canvas.gameObject.activeSelf != gameStarted)
                canvas.gameObject.SetActive(gameStarted);
            if (!gameStarted) return;

            // In-game touch shortcut: UP toggles the upgrade shop.
            if (MobileActionButton.WasPressed("Upgrades"))
            {
                var ui = FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
                if (ui != null) ui.ToggleUpgrades();
            }
            // Touch RESTART on win/lose screens.
            if (MobileActionButton.WasPressed("Restart") &&
                AdventureGameManager.Instance != null && AdventureGameManager.Instance.gameOver)
                AdventureGameManager.Instance.Restart();
            MobileActionButton.ClearPressed("Restart");

            // Menus (shop, win/lose) must receive touches: hide the gameplay
            // cluster so the look area can't swallow their taps.
            var uiNow = FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
            bool shopOpen = uiNow != null && uiNow.IsShopOpen();
            bool gameOver = AdventureGameManager.Instance != null && AdventureGameManager.Instance.gameOver;
            bool showGameplay = !shopOpen && !gameOver;
            foreach (var go in gameplayControls)
                if (go != null && go.activeSelf != showGameplay) go.SetActive(showGameplay);
            if (restartControl != null && restartControl.activeSelf != gameOver)
                restartControl.SetActive(gameOver);
            if (!showGameplay)
            {
                MobileActionButton.ClearAllPressed();
                player.SetMobileMove(Vector2.zero);
                player.SetMobileSprint(false);
                player.SetMobileBlock(false);
                return;
            }

            player.SetMobileMove(joystick != null ? joystick.Value : Vector2.zero);
            player.SetMobileSprint(MobileActionButton.IsHeld("Sprint"));
            player.SetMobileBlock(MobileActionButton.IsHeld("Guard"));

            if (cameraRig != null && lookArea != null)
                cameraRig.AddMobileLook(lookArea.ConsumeDelta());
        }

        void BuildCanvas()
        {
            var canvasObject = new GameObject("MobileControlsCanvas", typeof(RectTransform), typeof(Canvas), typeof(CanvasScaler), typeof(GraphicRaycaster));
            canvasObject.transform.SetParent(transform, false);
            canvas = canvasObject.GetComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            canvas.sortingOrder = 1500;

            var scaler = canvasObject.GetComponent<CanvasScaler>();
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            scaler.referenceResolution = new Vector2(1920f, 1080f);
            scaler.screenMatchMode = CanvasScaler.ScreenMatchMode.Expand;

            EnsureEventSystem();
            lookArea = CreateLookArea(canvasObject.transform);
            gameplayControls.Add(lookArea.gameObject);
            joystick = CreateJoystick(canvasObject.transform);
            gameplayControls.Add(joystick.gameObject);
            gameplayControls.Add(CreateActionButton(canvasObject.transform, "Attack", "ATTACK", new Vector2(0.84f, 0.22f), new Vector2(180f, 110f),
                () => { if (player != null) player.MobileAttack(); }).gameObject);
            gameplayControls.Add(CreateActionButton(canvasObject.transform, "Jump", "JUMP", new Vector2(0.69f, 0.35f), new Vector2(140f, 90f),
                () => { if (player != null) player.MobileJump(); }).gameObject);
            gameplayControls.Add(CreateActionButton(canvasObject.transform, "Sprint", "RUN", new Vector2(0.89f, 0.43f), new Vector2(140f, 90f), null).gameObject);
            gameplayControls.Add(CreateActionButton(canvasObject.transform, "Guard", "GUARD", new Vector2(0.69f, 0.19f), new Vector2(140f, 90f), null).gameObject);
            // Menu shortcuts: upgrade shop toggle + restart (win/lose screens).
            gameplayControls.Add(CreateActionButton(canvasObject.transform, "Upgrades", "UP", new Vector2(0.965f, 0.93f), new Vector2(130f, 90f), null).gameObject);
            restartControl = CreateActionButton(canvasObject.transform, "Restart", "RESTART", new Vector2(0.965f, 0.83f), new Vector2(130f, 90f), null).gameObject;
            restartControl.SetActive(false);
        }

        static void EnsureEventSystem()
        {
            if (FindAnyObjectByType<EventSystem>() != null) return;
            var eventSystem = new GameObject("MobileEventSystem", typeof(EventSystem), typeof(InputSystemUIInputModule));
            DontDestroyOnLoad(eventSystem);
        }

        static MobileLookArea CreateLookArea(Transform parent)
        {
            var go = new GameObject("LookArea", typeof(RectTransform), typeof(Image), typeof(MobileLookArea));
            go.transform.SetParent(parent, false);
            var rt = go.GetComponent<RectTransform>();
            rt.anchorMin = new Vector2(0.42f, 0f);
            rt.anchorMax = Vector2.one;
            rt.offsetMin = Vector2.zero;
            rt.offsetMax = Vector2.zero;
            go.GetComponent<Image>().color = new Color(1f, 1f, 1f, 0.001f);
            return go.GetComponent<MobileLookArea>();
        }

        static MobileJoystick CreateJoystick(Transform parent)
        {
            var baseObject = new GameObject("MoveStick", typeof(RectTransform), typeof(Image), typeof(MobileJoystick));
            baseObject.transform.SetParent(parent, false);
            var baseRect = baseObject.GetComponent<RectTransform>();
            baseRect.anchorMin = new Vector2(0f, 0f);
            baseRect.anchorMax = new Vector2(0f, 0f);
            baseRect.pivot = new Vector2(0f, 0f);
            baseRect.anchoredPosition = new Vector2(70f, 75f);
            baseRect.sizeDelta = new Vector2(250f, 250f);
            baseObject.GetComponent<Image>().color = new Color(0.08f, 0.1f, 0.14f, 0.6f);

            var handleObject = new GameObject("Handle", typeof(RectTransform), typeof(Image));
            handleObject.transform.SetParent(baseObject.transform, false);
            var handleRect = handleObject.GetComponent<RectTransform>();
            handleRect.anchorMin = new Vector2(0.5f, 0.5f);
            handleRect.anchorMax = new Vector2(0.5f, 0.5f);
            handleRect.sizeDelta = new Vector2(105f, 105f);
            handleObject.GetComponent<Image>().color = new Color(0.2f, 0.65f, 0.95f, 0.75f);

            var joystick = baseObject.GetComponent<MobileJoystick>();
            joystick.SetHandle(handleRect);
            return joystick;
        }

        static MobileActionButton CreateActionButton(Transform parent, string id, string label, Vector2 anchor, Vector2 size, Action pressed)
        {
            var go = new GameObject(id + "Button", typeof(RectTransform), typeof(Image), typeof(MobileActionButton));
            go.transform.SetParent(parent, false);
            var rt = go.GetComponent<RectTransform>();
            rt.anchorMin = anchor;
            rt.anchorMax = anchor;
            rt.sizeDelta = size;
            go.GetComponent<Image>().color = new Color(0.08f, 0.1f, 0.14f, 0.72f);
            var textObject = new GameObject("Label", typeof(RectTransform), typeof(Text));
            textObject.transform.SetParent(go.transform, false);
            var textRect = textObject.GetComponent<RectTransform>();
            textRect.anchorMin = Vector2.zero;
            textRect.anchorMax = Vector2.one;
            textRect.offsetMin = Vector2.zero;
            textRect.offsetMax = Vector2.zero;
            var text = textObject.GetComponent<Text>();
            text.text = label;
            text.alignment = TextAnchor.MiddleCenter;
            text.fontSize = 22;
            text.color = Color.white;
            text.font = Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf")
                ?? Resources.GetBuiltinResource<Font>("Arial.ttf");
            var btn = go.GetComponent<MobileActionButton>();
            btn.Configure(id, pressed);
            return btn;
        }
    }

    sealed class MobileJoystick : MonoBehaviour, IPointerDownHandler, IDragHandler, IPointerUpHandler
    {
        RectTransform handle;
        Vector2 value;
        float radius;

        public Vector2 Value => value;

        void OnDisable()
        {
            // Deactivated mid-drag (e.g. shop opened): release so the player doesn't drift.
            value = Vector2.zero;
            if (handle != null) handle.anchoredPosition = Vector2.zero;
        }

        public void SetHandle(RectTransform handleTransform)
        {
            handle = handleTransform;
            radius = 72f;
        }

        public void OnPointerDown(PointerEventData eventData) => UpdateValue(eventData);

        public void OnDrag(PointerEventData eventData) => UpdateValue(eventData);

        public void OnPointerUp(PointerEventData eventData)
        {
            value = Vector2.zero;
            if (handle != null) handle.anchoredPosition = Vector2.zero;
        }

        void UpdateValue(PointerEventData eventData)
        {
            if (handle == null) return;
            RectTransformUtility.ScreenPointToLocalPointInRectangle(
                transform as RectTransform,
                eventData.position,
                eventData.pressEventCamera,
                out var localPoint);
            var center = (transform as RectTransform).rect.center;
            var offset = Vector2.ClampMagnitude(localPoint - center, radius);
            value = offset / radius;
            handle.anchoredPosition = offset;
        }
    }

    sealed class MobileLookArea : MonoBehaviour, IPointerDownHandler, IDragHandler, IPointerUpHandler
    {
        Vector2 delta;

        public void OnPointerDown(PointerEventData eventData) { }

        public void OnDrag(PointerEventData eventData)
        {
            delta += eventData.delta * 0.35f;
        }

        public void OnPointerUp(PointerEventData eventData) { }

        public Vector2 ConsumeDelta()
        {
            var current = delta;
            delta = Vector2.zero;
            return current;
        }
    }

    sealed class MobileActionButton : MonoBehaviour, IPointerDownHandler, IPointerUpHandler, IPointerExitHandler
    {
        static readonly System.Collections.Generic.HashSet<string> HeldButtons = new();
        static readonly System.Collections.Generic.HashSet<string> PressedThisFrame = new();
        Action pressed;
        string id;

        public void Configure(string buttonId, Action onPressed)
        {
            id = buttonId;
            pressed = onPressed;
        }

        public void OnPointerDown(PointerEventData eventData)
        {
            HeldButtons.Add(id);
            PressedThisFrame.Add(id);
            pressed?.Invoke();
        }

        public void OnPointerUp(PointerEventData eventData) => HeldButtons.Remove(id);

        public void OnPointerExit(PointerEventData eventData) => HeldButtons.Remove(id);

        public static bool IsHeld(string buttonId) => HeldButtons.Contains(buttonId);

        /// <summary>True on the frame the button is touched (edge-triggered).</summary>
        public static bool WasPressed(string buttonId) => PressedThisFrame.Contains(buttonId);

        /// <summary>Clear an edge-triggered press after reading it.</summary>
        public static void ClearPressed(string buttonId) => PressedThisFrame.Remove(buttonId);

        public static void ClearAllPressed() => PressedThisFrame.Clear();
    }
}
