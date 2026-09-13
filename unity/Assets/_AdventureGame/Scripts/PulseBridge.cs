using UnityEngine;
using System.Collections;

namespace AdventureGame
{
    /// <summary>
    /// Bridges Pulse fitness data to the Unity arena.
    /// Receives PlayerSnapshot from Android and sends FightResult back.
    /// </summary>
    public class PulseBridge : MonoBehaviour
    {
        /// <summary>Must match CURRENT_BRIDGE_SCHEMA in the bridge module.</summary>
        public const int CurrentBridgeSchema = 1;

        public static PulseBridge Instance { get; private set; }
        
        [Header("Pulse Data (received from Android)")]
        public int cloneLevel = 1;
        public float strength = 50f;
        public float formMastery = 50f;
        public float consistency = 0.5f;
        public float recovery = 1f;
        public int gameXp = 0;
        public string playerId = "player_1";
        
        [Header("Debug")]
        public bool useTestData = false;
        
        void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }
            Instance = this;
            DontDestroyOnLoad(gameObject);
            
            // Use test data in editor if enabled
            if (useTestData && Application.isEditor)
            {
                LoadTestSnapshot();
            }
        }
        
        void Start()
        {
            // Wait a frame for GameManager to initialize
            StartCoroutine(InitializeBridge());
        }
        
        IEnumerator InitializeBridge()
        {
            yield return null; // Wait one frame
            
            // If we have data, apply it
            if (gameXp > 0 || strength != 50f)
            {
                ApplyToGameManager();
            }
        }
        
        /// <summary>
        /// Called from Android (Pulse) with player snapshot JSON.
        /// </summary>
        public void LoadPlayerSnapshot(string json)
        {
            Debug.Log("[PulseBridge] Received snapshot: " + json);
            
            var data = JsonUtility.FromJson<PlayerSnapshotData>(json);
            if (data == null)
            {
                Debug.LogError("[PulseBridge] Failed to parse snapshot JSON");
                return;
            }
            
            cloneLevel = data.cloneLevel;
            strength = data.strength;
            formMastery = data.formMastery;
            consistency = data.consistency;
            recovery = data.recovery;
            gameXp = data.gameXp;
            playerId = string.IsNullOrWhiteSpace(data.playerId) ? "player_1" : data.playerId;
            
            Debug.Log($"[PulseBridge] Loaded snapshot: Level {cloneLevel}, STR {strength}, XP {gameXp}");
            
            // Apply to game systems
            ApplyToGameManager();
        }
        
        void ApplyToGameManager()
        {
            var mgr = AdventureGameManager.Instance;
            if (mgr == null)
            {
                Debug.LogWarning("[PulseBridge] GameManager not found, will apply later");
                return;
            }
            
            // Convert fitness stats to game stats
            // Higher strength = more HP and ATK
            // Higher form mastery = more DEF
            
            int newHpLevel = Mathf.Clamp(Mathf.RoundToInt(strength / 25f), 0, 5);
            int newAtkLevel = Mathf.Clamp(Mathf.RoundToInt(strength / 30f), 0, 5);
            int newDefLevel = Mathf.Clamp(Mathf.RoundToInt(formMastery / 25f), 0, 5);
            
            mgr.hpLevel = newHpLevel;
            mgr.atkLevel = newAtkLevel;
            mgr.defLevel = newDefLevel;
            mgr.expPool = gameXp;
            
            // Recalculate max HP
            mgr.maxPlayerHealth = 100 + 25 * newHpLevel;
            
            Debug.Log($"[PulseBridge] Applied to GameManager: HP Lv{newHpLevel}, ATK Lv{newAtkLevel}, DEF Lv{newDefLevel}, XP {gameXp}");
        }
        
        /// <summary>
        /// Called from Unity when fight ends. Sends result back to Pulse.
        /// </summary>
        public void SendFightResult(string outcome, float duration)
        {
            var result = new FightResultData
            {
                schemaVersion = CurrentBridgeSchema,
                matchId = System.Guid.NewGuid().ToString("N"),
                playerId = playerId,
                opponentId = "arena_boss",
                outcome = outcome,
                playerPower = strength,
                playerSkill = formMastery / 100f,
                durationSec = Mathf.RoundToInt(duration),
                createdAtEpochMs = System.DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
            };
            
            string json = JsonUtility.ToJson(result);
            
            Debug.Log("[PulseBridge] Sending fight result: " + json);
            
            #if UNITY_ANDROID && !UNITY_EDITOR
            // Send back to Pulse via Android
            try
            {
                using (var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
                using (var activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity"))
                {
                    activity.Call("finishFight", json);
                }
            }
            catch (System.Exception e)
            {
                Debug.LogError("[PulseBridge] Failed to send result: " + e.Message);
            }
            #endif
        }
        
        /// <summary>
        /// Convenience method called by GameManager when fight ends.
        /// </summary>
        public void OnFightWon(float duration)
        {
            SendFightResult("player_won", duration);
        }
        
        public void OnFightLost(float duration)
        {
            SendFightResult("opponent_won", duration);
        }
        
        void LoadTestSnapshot()
        {
            cloneLevel = 3;
            strength = 75f;
            formMastery = 60f;
            consistency = 0.7f;
            recovery = 0.9f;
            gameXp = 150;
        }
        
        [System.Serializable]
        public class PlayerSnapshotData
        {
            public int schemaVersion;
            public string playerId;
            public int cloneLevel;
            public float strength;
            public float formMastery;
            public float consistency;
            public float recovery;
            public int gameXp;
            public string[] unlockedAbilities;
            public long updatedAtEpochMs;
        }
        
        [System.Serializable]
        public class FightResultData
        {
            public int schemaVersion;
            public string matchId;
            public string playerId;
            public string opponentId;
            public string outcome;
            public float playerPower;
            public float playerSkill;
            public int durationSec;
            public long createdAtEpochMs;
        }
    }
}
