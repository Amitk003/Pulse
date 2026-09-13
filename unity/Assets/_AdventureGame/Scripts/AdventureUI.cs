using UnityEngine;
using UnityEngine.UI;

namespace AdventureGame
{
    /// <summary>
    /// UGUI HUD: score, health, messages, win/lose panels.
    /// Builder creates it; Awake also self-heals missing children.
    /// </summary>
    public class AdventureUI : MonoBehaviour
    {
        Text scoreText;
        Image healthFill;
        Image staminaFill;
        Text messageText;
        GameObject messagePanel;
        GameObject winPanel;
        GameObject losePanel;
        GameObject bossPanel;
        Text bossNameText;
        Image bossFill;
        Image damageFlash;
        GameObject upgradePanel;
        Text upgradeExpText;
        Text expBarText;
        Image expBarFill;
        readonly Text[] upgradeLabels = new Text[3];
        readonly Button[] upgradeButtons = new Button[3];
        GameObject playNowGo;
        float messageTimer;

        void Awake()
        {
            EnsureBuilt();
        }

        void Update()
        {
            if (messageTimer > 0f)
            {
                messageTimer -= Time.deltaTime;
                if (messageTimer <= 0f && messagePanel != null) messagePanel.SetActive(false);
            }
            if (damageFlash != null)
            {
                var c = damageFlash.color;
                if (c.a > 0f)
                {
                    c.a = Mathf.Max(0f, c.a - Time.unscaledDeltaTime * 1.5f);
                    damageFlash.color = c;
                }
            }
            if (UnityEngine.InputSystem.Keyboard.current != null &&
                UnityEngine.InputSystem.Keyboard.current.uKey.wasPressedThisFrame)
                ToggleUpgrades();
        }

        public void DamageFlash()
        {
            EnsureBuilt();
            if (damageFlash == null) return;
            var c = damageFlash.color;
            c.a = 0.45f;
            damageFlash.color = c;
        }

        public void SetScore(int collected, int total, int kills, int exp = 0)
        {
            if (scoreText != null) scoreText.text = $"Shards {collected}/{total}   Kills {kills}   EXP {exp}";
            UpdateExpBar(exp);
        }

        void UpdateExpBar(int exp)
        {
            if (expBarText != null) expBarText.text = $"EXP {exp}";
            if (expBarFill != null)
            {
                // Fill toward the cheapest next upgrade.
                var mgr = AdventureGameManager.Instance;
                int minLevel = mgr != null ? Mathf.Min(mgr.TrackLevel(0), mgr.TrackLevel(1), mgr.TrackLevel(2)) : 0;
                int cost = AdventureGameManager.UpgradeCost(Mathf.Min(minLevel, AdventureGameManager.MaxUpgradeLevel - 1));
                expBarFill.rectTransform.anchorMax = new Vector2(Mathf.Clamp01(exp / (float)cost), 1f);
            }
        }

        public void SetStamina(float frac, bool blocking)
        {
            if (staminaFill == null) return;
            staminaFill.rectTransform.anchorMax = new Vector2(Mathf.Clamp01(frac), 1f);
            staminaFill.color = blocking ? new Color(0.3f, 0.7f, 1f) : new Color(0.3f, 0.7f, 1f, 0.55f);
        }

        public void SetHealth(int hp, int max)
        {
            if (healthFill == null) return;
            float f = max > 0 ? Mathf.Clamp01(hp / (float)max) : 0f;
            healthFill.rectTransform.anchorMax = new Vector2(f, 1f);
            healthFill.color = f > 0.5f ? Color.green : f > 0.25f ? Color.yellow : Color.red;
        }

        public void ShowMessage(string msg, float duration = 2.5f)
        {
            EnsureBuilt();
            messageText.text = msg;
            messagePanel.SetActive(true);
            messageTimer = duration;
        }

        public void ShowWin(int collected, int total, int kills, float time, string title = "YOU WIN!")
        {
            EnsureBuilt();
            winPanel.SetActive(true);
            var t = winPanel.GetComponentInChildren<Text>();
            if (t != null) t.text = $"{title}\nShards {collected}/{total}  Kills {kills}  Time {time:F1}s\n{RestartHint()}";
        }

        public void ShowLose(int collected, int total)
        {
            EnsureBuilt();
            losePanel.SetActive(true);
            var t = losePanel.GetComponentInChildren<Text>();
            if (t != null) t.text = $"YOU DIED\nShards {collected}/{total}\n{RestartHint()}";
        }

        static string RestartHint() => Application.isMobilePlatform
            ? "Tap RESTART to try again"
            : "Press R restart • U upgrades";

        void EnsureBuilt()
        {
            if (scoreText != null) return;
            var canvas = GetComponentInChildren<Canvas>();
            if (canvas == null) return;

            scoreText = FindText(canvas.transform, "ScoreText");
            if (scoreText == null) scoreText = MakeLabel(canvas.transform, "ScoreText", "Shards 0/0", 16, new Vector2(290f, -10f), TextAnchor.UpperLeft);

            var hpObj = canvas.transform.Find("HealthSlider");
            if (hpObj != null) Object.Destroy(hpObj.gameObject); // legacy broken slider
            if (healthFill == null)
                healthFill = MakeBar(canvas.transform, "HealthBar",
                    new Vector2(0f, 1f), new Vector2(0f, 1f),
                    new Vector2(120f, -55f), new Vector2(220f, 20f), Color.green);
            if (staminaFill == null)
                staminaFill = MakeBar(canvas.transform, "StaminaBar",
                    new Vector2(0f, 1f), new Vector2(0f, 1f),
                    new Vector2(120f, -80f), new Vector2(220f, 10f), new Color(0.3f, 0.7f, 1f));

            if (expBarFill == null)
            {
                var go = new GameObject("ExpBar", typeof(RectTransform), typeof(Image));
                go.transform.SetParent(canvas.transform, false);
                var rt = go.GetComponent<RectTransform>();
                rt.anchorMin = new Vector2(1f, 1f); rt.anchorMax = new Vector2(1f, 1f);
                rt.anchoredPosition = new Vector2(-120f, -55f); rt.sizeDelta = new Vector2(220f, 22f);
                go.GetComponent<Image>().color = new Color(0f, 0f, 0f, 0.6f);
                var fill = new GameObject("Fill", typeof(RectTransform), typeof(Image));
                fill.transform.SetParent(go.transform, false);
                var frt = fill.GetComponent<RectTransform>();
                frt.anchorMin = Vector2.zero; frt.anchorMax = Vector2.one;
                frt.offsetMin = Vector2.zero; frt.offsetMax = Vector2.zero;
                var img = fill.GetComponent<Image>();
                img.color = new Color(1f, 0.85f, 0.3f);
                expBarFill = img;
                var label = MakeLabel(go.transform, "ExpText", "EXP 0", 14, Vector2.zero, TextAnchor.MiddleCenter, new Vector2(220f, 22f));
                label.alignment = TextAnchor.MiddleCenter;
                label.rectTransform.anchorMin = Vector2.zero; label.rectTransform.anchorMax = Vector2.one;
                label.rectTransform.offsetMin = Vector2.zero; label.rectTransform.offsetMax = Vector2.zero;
                expBarText = label;
            }

            if (damageFlash == null)
            {
                var go = new GameObject("DamageFlash", typeof(RectTransform), typeof(Image));
                go.transform.SetParent(canvas.transform, false);
                var rt = go.GetComponent<RectTransform>();
                rt.anchorMin = Vector2.zero; rt.anchorMax = Vector2.one;
                rt.offsetMin = Vector2.zero; rt.offsetMax = Vector2.zero;
                damageFlash = go.GetComponent<Image>();
                damageFlash.color = new Color(0.8f, 0.05f, 0.05f, 0f);
                damageFlash.raycastTarget = false;
            }

            messagePanel = canvas.transform.Find("MessagePanel")?.gameObject;
            if (messagePanel == null)
            {
                messagePanel = new GameObject("MessagePanel", typeof(RectTransform), typeof(Image));
                var rt = messagePanel.GetComponent<RectTransform>();
                rt.SetParent(canvas.transform, false);
                rt.anchorMin = new Vector2(0.5f, 0f); rt.anchorMax = new Vector2(0.5f, 0f);
                rt.anchoredPosition = new Vector2(0f, 90f); rt.sizeDelta = new Vector2(560f, 60f);
                messageText = MakeLabel(messagePanel.transform, "MessageText", "", 18, Vector2.zero, TextAnchor.MiddleCenter);
                messageText.alignment = TextAnchor.MiddleCenter;
                messageText.rectTransform.anchorMin = Vector2.zero; messageText.rectTransform.anchorMax = Vector2.one;
                messageText.rectTransform.offsetMin = Vector2.zero; messageText.rectTransform.offsetMax = Vector2.zero;
                messagePanel.SetActive(false);
            }
            else messageText = messagePanel.GetComponentInChildren<Text>();

            winPanel = canvas.transform.Find("WinPanel")?.gameObject;
            if (winPanel == null) winPanel = MakeEndPanel(canvas.transform, "WinPanel", "YOU WIN!", Color.green);
            losePanel = canvas.transform.Find("LosePanel")?.gameObject;
            if (losePanel == null) losePanel = MakeEndPanel(canvas.transform, "LosePanel", "YOU DIED", Color.red);

            if (bossPanel == null)
            {
                bossPanel = new GameObject("BossPanel", typeof(RectTransform), typeof(Image));
                bossPanel.transform.SetParent(canvas.transform, false);
                var rt = bossPanel.GetComponent<RectTransform>();
                rt.anchorMin = new Vector2(0.5f, 1f); rt.anchorMax = new Vector2(0.5f, 1f);
                rt.anchoredPosition = new Vector2(0f, -30f); rt.sizeDelta = new Vector2(420f, 56f);
                bossPanel.GetComponent<Image>().color = new Color(0f, 0f, 0f, 0.6f);
                bossNameText = MakeLabel(bossPanel.transform, "BossName", "BOSS", 18, new Vector2(210f, -16f), TextAnchor.MiddleCenter);
                bossNameText.alignment = TextAnchor.MiddleCenter;
                bossNameText.color = new Color(1f, 0.3f, 0.25f);
                bossFill = MakeBar(bossPanel.transform, "BossHP",
                    new Vector2(0.5f, 0f), new Vector2(0.5f, 0f),
                    new Vector2(0f, 12f), new Vector2(380f, 16f), new Color(0.9f, 0.15f, 0.1f));
                bossPanel.SetActive(false);
            }

            if (upgradePanel == null)
            {
                upgradePanel = new GameObject("UpgradePanel", typeof(RectTransform), typeof(Image));
                upgradePanel.transform.SetParent(canvas.transform, false);
                var rt = upgradePanel.GetComponent<RectTransform>();
                rt.anchorMin = new Vector2(0.5f, 0.5f); rt.anchorMax = new Vector2(0.5f, 0.5f);
                rt.sizeDelta = new Vector2(470f, 330f);
                upgradePanel.GetComponent<Image>().color = new Color(0f, 0f, 0f, 0.88f);
                var title = MakeLabel(upgradePanel.transform, "UpgradeTitle", "UPGRADES", 28, new Vector2(235f, -28f), TextAnchor.MiddleCenter, new Vector2(430f, 40f));
                title.alignment = TextAnchor.MiddleCenter;
                title.color = new Color(1f, 0.85f, 0.3f);
                upgradeExpText = MakeLabel(upgradePanel.transform, "UpgradeEXP", "EXP: 0", 22, new Vector2(235f, -66f), TextAnchor.MiddleCenter, new Vector2(430f, 30f));
                upgradeExpText.alignment = TextAnchor.MiddleCenter;
                var hint = MakeLabel(upgradePanel.transform, "UpgradeHint",
                    $"Coins +20 EXP  •  Boss +{AdventureGameManager.BossExpReward} EXP", 15, new Vector2(235f, -94f), TextAnchor.MiddleCenter, new Vector2(430f, 22f));
                hint.alignment = TextAnchor.MiddleCenter;
                hint.color = new Color(0.9f, 0.9f, 0.9f);
                for (int i = 0; i < 3; i++)
                {
                    int track = i;
                    // anchoredPosition = rect CENTER (pivot 0.5): 165 puts the left edge at x=10 inside the 470-wide panel.
                    var rowLabel = MakeLabel(upgradePanel.transform, $"UpgradeRow{i}", "", 16, new Vector2(165f, -118f - i * 52f), TextAnchor.MiddleLeft, new Vector2(310f, 48f));
                    upgradeLabels[i] = rowLabel;
                    var btnGo = new GameObject($"Buy{i}", typeof(RectTransform), typeof(Image), typeof(Button));
                    btnGo.transform.SetParent(upgradePanel.transform, false);
                    var btnRt = btnGo.GetComponent<RectTransform>();
                    btnRt.anchorMin = new Vector2(1f, 1f); btnRt.anchorMax = new Vector2(1f, 1f);
                    btnRt.anchoredPosition = new Vector2(-70f, -118f - i * 52f); btnRt.sizeDelta = new Vector2(120f, 40f);
                    btnGo.GetComponent<Image>().color = new Color(0.2f, 0.6f, 1f);
                    var btnLabel = MakeLabel(btnGo.transform, "Text", "BUY", 16, Vector2.zero, TextAnchor.MiddleCenter);
                    btnLabel.alignment = TextAnchor.MiddleCenter;
                    btnLabel.rectTransform.anchorMin = new Vector2(0.5f, 0.5f);
                    btnLabel.rectTransform.anchorMax = new Vector2(0.5f, 0.5f);
                    btnLabel.rectTransform.anchoredPosition = Vector2.zero;
                    var btn = btnGo.GetComponent<Button>();
                    btn.navigation = new Navigation { mode = Navigation.Mode.None };
                    btn.onClick.AddListener(() =>
                    {
                        var mgr = AdventureGameManager.Instance;
                        if (mgr != null && mgr.TryBuy(track)) RefreshUpgrades();
                        else RefreshUpgrades();
                    });
                    upgradeButtons[i] = btn;
                }
                var closeGo = new GameObject("CloseUpgrades", typeof(RectTransform), typeof(Image), typeof(Button));
                closeGo.transform.SetParent(upgradePanel.transform, false);
                var closeRt = closeGo.GetComponent<RectTransform>();
                closeRt.anchorMin = new Vector2(0.5f, 0f); closeRt.anchorMax = new Vector2(0.5f, 0f);
                closeRt.anchoredPosition = new Vector2(-110f, 28f); closeRt.sizeDelta = new Vector2(160f, 40f);
                closeGo.GetComponent<Image>().color = new Color(0.4f, 0.4f, 0.4f);
                var closeLabel = MakeLabel(closeGo.transform, "Text", "Close (U)", 16, Vector2.zero, TextAnchor.MiddleCenter);
                closeLabel.alignment = TextAnchor.MiddleCenter;
                closeLabel.rectTransform.anchorMin = new Vector2(0.5f, 0.5f);
                closeLabel.rectTransform.anchorMax = new Vector2(0.5f, 0.5f);
                closeLabel.rectTransform.anchoredPosition = Vector2.zero;
                var closeBtn = closeGo.GetComponent<Button>();
                closeBtn.navigation = new Navigation { mode = Navigation.Mode.None };
                closeBtn.onClick.AddListener(() => ShowUpgrades(false));
                playNowGo = new GameObject("PlayNow", typeof(RectTransform), typeof(Image), typeof(Button));
                playNowGo.transform.SetParent(upgradePanel.transform, false);
                var playNowRt = playNowGo.GetComponent<RectTransform>();
                playNowRt.anchorMin = new Vector2(0.5f, 0f); playNowRt.anchorMax = new Vector2(0.5f, 0f);
                playNowRt.anchoredPosition = new Vector2(110f, 28f); playNowRt.sizeDelta = new Vector2(160f, 40f);
                playNowGo.GetComponent<Image>().color = new Color(0.2f, 0.65f, 0.25f);
                var playNowLabel = MakeLabel(playNowGo.transform, "Text", "PLAY NOW", 18, Vector2.zero, TextAnchor.MiddleCenter);
                playNowLabel.alignment = TextAnchor.MiddleCenter;
                playNowLabel.rectTransform.anchorMin = new Vector2(0.5f, 0.5f);
                playNowLabel.rectTransform.anchorMax = new Vector2(0.5f, 0.5f);
                playNowLabel.rectTransform.anchoredPosition = Vector2.zero;
                var playNowBtn = playNowGo.GetComponent<Button>();
                playNowBtn.navigation = new Navigation { mode = Navigation.Mode.None };
                playNowBtn.onClick.AddListener(() =>
                {
                    ShowUpgrades(false);
                    var start = Object.FindAnyObjectByType<StartScreen>(FindObjectsInactive.Exclude);
                    if (start != null) start.Play();
                });
                playNowGo.SetActive(false);
                upgradePanel.SetActive(false);
            }
        }

        public void ToggleUpgrades()
        {
            EnsureBuilt();
            if (upgradePanel == null) return;
            ShowUpgrades(!upgradePanel.activeSelf);
        }

        public bool IsShopOpen()
        {
            return upgradePanel != null && upgradePanel.activeSelf;
        }

        public void ShowUpgrades(bool show)
        {
            EnsureBuilt();
            if (upgradePanel == null) return;
            if (show) RefreshUpgrades();
            upgradePanel.SetActive(show);
            if (UnityEngine.EventSystems.EventSystem.current != null)
                UnityEngine.EventSystems.EventSystem.current.SetSelectedGameObject(null);
        }

        public void RefreshUpgrades()
        {
            var mgr = AdventureGameManager.Instance;
            if (upgradePanel == null || mgr == null) return;
            if (upgradeExpText != null) upgradeExpText.text = $"EXP: {mgr.expPool}";
            UpdateExpBar(mgr.expPool);
            var start = Object.FindAnyObjectByType<StartScreen>(FindObjectsInactive.Exclude);
            if (playNowGo != null) playNowGo.SetActive(start != null && !start.HasStarted());
            for (int i = 0; i < 3; i++)
            {
                int level = mgr.TrackLevel(i);
                if (upgradeLabels[i] != null)
                    upgradeLabels[i].text = $"{AdventureGameManager.TrackName(i)}  Lv{level}/{AdventureGameManager.MaxUpgradeLevel}\n{AdventureGameManager.TrackDesc(i, level)}";
                if (upgradeButtons[i] != null)
                {
                    bool maxed = level >= AdventureGameManager.MaxUpgradeLevel;
                    bool afford = mgr.expPool >= AdventureGameManager.UpgradeCost(level);
                    upgradeButtons[i].interactable = !maxed && afford;
                    var label = upgradeButtons[i].GetComponentInChildren<Text>();
                    if (label != null) label.text = maxed ? "MAX" : $"{AdventureGameManager.UpgradeCost(level)} EXP";
                }
            }
        }

        public void ShowBossHP(string bossName, int hp, int max)
        {
            EnsureBuilt();
            if (bossPanel == null) return;
            bossPanel.SetActive(true);
            if (bossNameText != null) bossNameText.text = bossName;
            if (bossFill != null)
                bossFill.rectTransform.anchorMax = new Vector2(max > 0 ? Mathf.Clamp01(hp / (float)max) : 0f, 1f);
        }

        public void HideBoss()
        {
            if (bossPanel != null) bossPanel.SetActive(false);
        }

        static Text FindText(Transform root, string name)
        {
            var f = root.Find(name);
            // Also search one level deep (e.g. inside MessagePanel).
            if (f == null)
            {
                foreach (Transform child in root)
                {
                    f = child.Find(name);
                    if (f != null) break;
                }
            }
            return f != null ? f.GetComponent<Text>() : null;
        }

        static Text MakeLabel(Transform parent, string name, string text, int size, Vector2 anchoredPos, TextAnchor anchor, Vector2? sizeOverride = null)
        {
            var go = new GameObject(name, typeof(RectTransform), typeof(Text));
            go.transform.SetParent(parent, false);
            var rt = go.GetComponent<RectTransform>();
            rt.anchorMin = new Vector2(0f, 1f); rt.anchorMax = new Vector2(0f, 1f);
            rt.anchoredPosition = anchoredPos; rt.sizeDelta = sizeOverride ?? new Vector2(560f, 40f);
            var t = go.GetComponent<Text>();
            t.text = text; t.fontSize = size; t.alignment = anchor;
            t.font = Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf")
                  ?? Resources.GetBuiltinResource<Font>("Arial.ttf");
            t.color = Color.white;
            var outline = go.AddComponent<Outline>();
            outline.effectColor = Color.black;
            return t;
        }

        // Image-fraction bar: background + fill stretched by anchorMax.x.
        // (Unity's Slider needs its full template to look right; this can't break.)
        static Image MakeBar(Transform parent, string name, Vector2 anchorMin, Vector2 anchorMax,
            Vector2 anchoredPos, Vector2 size, Color fillColor)
        {
            var go = new GameObject(name, typeof(RectTransform), typeof(Image));
            go.transform.SetParent(parent, false);
            var rt = go.GetComponent<RectTransform>();
            rt.anchorMin = anchorMin; rt.anchorMax = anchorMax;
            rt.anchoredPosition = anchoredPos; rt.sizeDelta = size;
            go.GetComponent<Image>().color = new Color(0f, 0f, 0f, 0.6f);
            var fill = new GameObject("Fill", typeof(RectTransform), typeof(Image));
            fill.transform.SetParent(go.transform, false);
            var frt = fill.GetComponent<RectTransform>();
            frt.anchorMin = Vector2.zero; frt.anchorMax = Vector2.one;
            frt.offsetMin = Vector2.zero; frt.offsetMax = Vector2.zero;
            var img = fill.GetComponent<Image>();
            img.color = fillColor;
            return img;
        }

        GameObject MakeEndPanel(Transform parent, string name, string title, Color accent)
        {
            var go = new GameObject(name, typeof(RectTransform), typeof(Image));
            go.transform.SetParent(parent, false);
            var rt = go.GetComponent<RectTransform>();
            rt.anchorMin = new Vector2(0.5f, 0.5f); rt.anchorMax = new Vector2(0.5f, 0.5f);
            rt.sizeDelta = new Vector2(420f, 240f);
            go.GetComponent<Image>().color = new Color(0f, 0f, 0f, 0.85f);
            var label = MakeLabel(go.transform, name + "Text", title, 24, new Vector2(210f, -70f), TextAnchor.MiddleCenter);
            label.alignment = TextAnchor.MiddleCenter;
            label.color = accent;

            var btnGo = new GameObject("RestartButton", typeof(RectTransform), typeof(Image), typeof(Button));
            btnGo.transform.SetParent(go.transform, false);
            var btnRt = btnGo.GetComponent<RectTransform>();
            btnRt.anchorMin = new Vector2(0.5f, 0f); btnRt.anchorMax = new Vector2(0.5f, 0f);
            btnRt.anchoredPosition = new Vector2(0f, 30f); btnRt.sizeDelta = new Vector2(200f, 50f);
            btnGo.GetComponent<Image>().color = new Color(0.2f, 0.6f, 1f);
            var btnLabel = MakeLabel(btnGo.transform, "Text", "Restart (R)", 18, Vector2.zero, TextAnchor.MiddleCenter);
            btnLabel.alignment = TextAnchor.MiddleCenter;
            btnLabel.rectTransform.anchorMin = new Vector2(0.5f, 0.5f);
            btnLabel.rectTransform.anchorMax = new Vector2(0.5f, 0.5f);
            btnLabel.rectTransform.anchoredPosition = Vector2.zero;
            var endBtn = btnGo.GetComponent<Button>();
            endBtn.navigation = new Navigation { mode = Navigation.Mode.None };
            endBtn.onClick.AddListener(() =>
            {
                if (AdventureGameManager.Instance != null) AdventureGameManager.Instance.Restart();
            });
            // R to restart
            go.AddComponent<RestartKey>();
            go.SetActive(false);
            return go;
        }
    }

    /// <summary>Press R to restart after win/lose.</summary>
    public class RestartKey : MonoBehaviour
    {
        void Update()
        {
            // R doubles as gun-reload during play: only restart once the game is over.
            if (AdventureGameManager.Instance == null || !AdventureGameManager.Instance.gameOver) return;
            bool r = UnityEngine.InputSystem.Keyboard.current != null &&
                     UnityEngine.InputSystem.Keyboard.current.rKey.wasPressedThisFrame;
#if ENABLE_LEGACY_INPUT_MANAGER
            r |= Input.GetKeyDown(KeyCode.R);
#endif
            if (r && AdventureGameManager.Instance != null)
                AdventureGameManager.Instance.Restart();
        }
    }
}
