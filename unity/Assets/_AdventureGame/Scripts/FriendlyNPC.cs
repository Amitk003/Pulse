using UnityEngine;

namespace AdventureGame
{
    /// <summary>
    /// Friendly Y-Bot NPC: idles, faces the player when near, gives quest text
    /// and a heal on Interact (E / Player/Interact action).
    /// Attach to a clone of the Statter Player_Model Prefab.
    /// </summary>
    public class FriendlyNPC : MonoBehaviour
    {
        public string displayName = "Y-Bot";
        [TextArea(2, 4)]
        public string[] dialogue = new string[]
        {
            "Hey! The shards scattered when the portal broke.",
            "Collect all 5 shards, then reach the CYAN goal gate. Press E to talk. Enemies hate left-click!",
        };
        public int healOnTalk = 25;
        public float faceRange = 6f;

        int line;
        Animator animator;
        Transform player;
        static readonly int HashY = Animator.StringToHash("Y");

        void Awake()
        {
            animator = GetComponent<Animator>();
            // NPC should never move with root motion.
            if (animator != null) animator.applyRootMotion = false;
            // Remove gameplay components if this was cloned from the player prefab.
            var pc = GetComponent<AdventurePlayerController>();
            if (pc != null) Destroy(pc);
            var old = GetComponent("PlayerController") as MonoBehaviour;
            if (old != null) Destroy(old);
        }

        void Update()
        {
            if (animator != null) animator.SetFloat(HashY, 0f); // idle
            if (player == null)
            {
                var p = GameObject.FindGameObjectWithTag("Player");
                if (p != null) player = p.transform;
            }
            if (player != null && Vector3.Distance(transform.position, player.position) <= faceRange)
            {
                Vector3 to = player.position - transform.position;
                to.y = 0f;
                if (to.sqrMagnitude > 0.01f)
                    transform.rotation = Quaternion.Slerp(transform.rotation,
                        Quaternion.LookRotation(to.normalized), Time.deltaTime * 5f);
            }
        }

        public void Interact()
        {
            var ui = Object.FindAnyObjectByType<AdventureUI>(FindObjectsInactive.Exclude);
            string msg = dialogue.Length > 0 ? dialogue[line % dialogue.Length] : "Hello!";
            line++;
            if (ui != null) ui.ShowMessage($"{displayName}: {msg}", 4f);
            if (AdventureGameManager.Instance != null && healOnTalk > 0)
                AdventureGameManager.Instance.HealPlayer(healOnTalk);
        }

        void OnDrawGizmosSelected()
        {
            Gizmos.color = Color.cyan;
            Gizmos.DrawWireSphere(transform.position, faceRange);
        }
    }
}
