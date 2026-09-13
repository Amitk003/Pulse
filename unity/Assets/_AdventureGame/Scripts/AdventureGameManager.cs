using UnityEngine;
using UnityEngine.SceneManagement;

namespace AdventureGame
{
    /// <summary>
    /// Scene-local game state: collectibles, player health, enemies, win/lose.
    /// Wired automatically by Adventure/Build Prototype Level.
    /// </summary>
    public class AdventureGameManager : MonoBehaviour
    {
        public static AdventureGameManager Instance { get; private set; }

        [Header("Player")]
        public GameObject player;
        public Vector3 spawnPoint = new Vector3(0f, 1f, 0f);

        [Header("Rules")]
        public int maxPlayerHealth = 100;
        public int totalCollectibles;
        public int collected;
        public int enemiesDefeated;
        public bool requireGoalToWin = true;
        public bool gameOver;

        [Header("Progression (EXP persists between runs)")]
        public int expPool;
        public int hpLevel;
        public int atkLevel;
        public int defLevel;

        /// <summary>Exp for defeating the boss, awarded once on its death.</summary>
        public const int BossExpReward = 200;
        public const int MaxUpgradeLevel = 5;
        const string KExp = "Adventure_EXP";
        const string KHp = "Adventure_Up_HP";
        const string KAtk = "Adventure_Up_ATK";
        const string KDef = "Adventure_Up_DEF";

        [Header("Sounds (auto-wired by builder)")]
        public AudioClip pickupSound;

        int playerHealth;
        float startTime;
        AdventureUI ui;
        AudioSource sfx;

        void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }
            Instance = this;
            LoadProgression();
            playerHealth = maxPlayerHealth;
            startTime = Time.time;
            sfx = gameObject.AddComponent<AudioSource>();
            sfx.playOnAwake = false;
        }

        // ---------- progression ----------

        void LoadProgression()
        {
            expPool = PlayerPrefs.GetInt(KExp, 0);
            hpLevel = Mathf.Clamp(PlayerPrefs.GetInt(KHp, 0), 0, MaxUpgradeLevel);
            atkLevel = Mathf.Clamp(PlayerPrefs.GetInt(KAtk, 0), 0, MaxUpgradeLevel);
            defLevel = Mathf.Clamp(PlayerPrefs.GetInt(KDef, 0), 0, MaxUpgradeLevel);
            maxPlayerHealth = 100 + 25 * hpLevel;
        }

        void SaveProgression()
        {
            PlayerPrefs.SetInt(KExp, expPool);
            PlayerPrefs.SetInt(KHp, hpLevel);
            PlayerPrefs.SetInt(KAtk, atkLevel);
            PlayerPrefs.SetInt(KDef, defLevel);
            PlayerPrefs.Save();
        }

        public float AttackMultiplier() => 1f + 0.2f * atkLevel;
        public float DefenseMultiplier() => Mathf.Pow(0.88f, defLevel);
        public static int UpgradeCost(int level) => 50 * (level + 1);

        public int TrackLevel(int track) =>
            track == 0 ? hpLevel : track == 1 ? atkLevel : defLevel;

        public static string TrackName(int track) =>
            track == 0 ? "Health" : track == 1 ? "Attack" : "Defense";

        public static string TrackDesc(int track, int level) =>
            track == 0 ? $"+25 Max HP per level (now {100 + 25 * level})"
            : track == 1 ? $"+20% punch damage (now x{1f + 0.2f * level:F1})"
            : $"-12% damage taken (now x{Mathf.Pow(0.88f, level):F2})";

        /// <summary>track: 0 Health, 1 Attack, 2 Defense. Works mid-run and on end screens.</summary>
        public bool TryBuy(int track)
        {
            int level = TrackLevel(track);
            if (level >= MaxUpgradeLevel) return false;
            int cost = UpgradeCost(level);
            if (expPool < cost) return false;
            expPool -= cost;
            if (track == 0) { hpLevel++; maxPlayerHealth = 100 + 25 * hpLevel; playerHealth = maxPlayerHealth; }
            else if (track == 1) atkLevel++;
            else defLevel++;
            SaveProgression();
            UpdateHUD();
            if (ui == null) ui = Object.FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
            if (ui != null) ui.RefreshUpgrades();
            return true;
        }

        public void AddEXP(int amount, bool showMsg)
        {
            if (gameOver) return;
            expPool += amount;
            SaveProgression();
            UpdateHUD();
            if (showMsg)
            {
                if (ui == null) ui = Object.FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
                if (ui != null) ui.ShowMessage($"+{amount} EXP", 1.5f);
            }
        }

        void Start()
        {
            ui = Object.FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
            if (player == null)
            {
                var pc = Object.FindAnyObjectByType<AdventurePlayerController>(FindObjectsInactive.Exclude);
                if (pc != null) player = pc.gameObject;
                else
                {
                    var tagged = GameObject.FindGameObjectWithTag("Player");
                    if (tagged != null) player = tagged;
                }
            }
            if (player != null) spawnPoint = player.transform.position;
            totalCollectibles = Object.FindObjectsByType<Collectible>(FindObjectsInactive.Exclude).Length;
            UpdateHUD();
        }

        void UpdateHUD()
        {
            if (ui == null) ui = Object.FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
            if (ui != null)
            {
                ui.SetScore(collected, totalCollectibles, enemiesDefeated, expPool);
                ui.SetHealth(playerHealth, maxPlayerHealth);
            }
        }

        public void OnCollectiblePicked(Collectible c)
        {
            if (gameOver || c == null) return;
            collected += c.scoreValue;
            if (c.healAmount > 0) HealPlayer(c.healAmount, false);
            AddEXP(20, false); // quiet: shard message covers feedback
            if (sfx != null && pickupSound != null) sfx.PlayOneShot(pickupSound);
            UpdateHUD();
            if (ui != null) ui.ShowMessage($"Shard {collected}/{totalCollectibles}", 1.5f);
            if (!requireGoalToWin && collected >= totalCollectibles) Win();
        }

        public void OnEnemyDefeated(EnemyChaser e)
        {
            if (gameOver) return;
            enemiesDefeated++;
            UpdateHUD();
            if (ui != null) ui.ShowMessage("Enemy defeated!", 1.5f);
        }

        public void OnBossDefeated()
        {
            if (gameOver) return;
            enemiesDefeated++;
            AddEXP(BossExpReward, true);
            Win("BOSS SLAIN!");
        }

        public bool CanWin()
        {
            return collected >= totalCollectibles;
        }

        public void OnGoalReached()
        {
            if (gameOver) return;
            if (CanWin()) Win();
            else if (ui != null) ui.ShowMessage($"Find shards first! {collected}/{totalCollectibles}", 2.5f);
        }

        public void DamagePlayer(int amount)
        {
            if (gameOver) return;
            var pc = player != null ? player.GetComponent<AdventurePlayerController>() : null;
            if (pc != null && pc.IsBlocking)
            {
                pc.OnBlockedHit(amount); // shield absorbs it all
                return;
            }
            playerHealth = Mathf.Max(0, playerHealth - Mathf.Max(1, Mathf.RoundToInt(amount * DefenseMultiplier())));
            UpdateHUD();
            if (ui != null) ui.DamageFlash();
            if (pc != null) pc.PlayHit();
            if (playerHealth <= 0) Lose();
        }

        public void HealPlayer(int amount, bool showMsg = true)
        {
            playerHealth = Mathf.Min(maxPlayerHealth, playerHealth + amount);
            UpdateHUD();
            if (showMsg && ui != null) ui.ShowMessage($"+{amount} HP", 1.5f);
        }

        public void RespawnPlayer()
        {
            if (player == null) return;
            var cc = player.GetComponent<CharacterController>();
            if (cc != null) cc.enabled = false;
            player.transform.position = spawnPoint;
            player.transform.rotation = Quaternion.identity;
            if (cc != null) cc.enabled = true;
            var pc = player.GetComponent<AdventurePlayerController>();
            if (pc != null) pc.ResetVerticalVelocity();
        }

        public void Win(string title = "YOU WIN!")
        {
            if (gameOver) return;
            gameOver = true;
            float t = Time.time - startTime;
            if (ui == null) ui = Object.FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
            if (ui != null) ui.ShowWin(collected, totalCollectibles, enemiesDefeated, t, title);
            UnlockCursor();
            
            // Notify PulseBridge if present
            var bridge = PulseBridge.Instance;
            if (bridge != null) bridge.OnFightWon(t);
        }

        public void Lose()
        {
            if (gameOver) return;
            gameOver = true;
            float t = Time.time - startTime;
            if (ui == null) ui = Object.FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
            if (ui != null) ui.ShowLose(collected, totalCollectibles);
            UnlockCursor();
            
            // Notify PulseBridge if present
            var bridge = PulseBridge.Instance;
            if (bridge != null) bridge.OnFightLost(t);
        }

        public void Restart()
        {
            Time.timeScale = 1f;
            SceneManager.LoadScene(SceneManager.GetActiveScene().buildIndex);
        }

        void UnlockCursor()
        {
            Cursor.lockState = CursorLockMode.None;
            Cursor.visible = true;
        }
    }
}
