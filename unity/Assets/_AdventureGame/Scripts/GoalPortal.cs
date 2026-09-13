using UnityEngine;

namespace AdventureGame
{
    /// <summary>
    /// Cyan goal gate: wins when all shards are collected, otherwise hints.
    /// </summary>
    [RequireComponent(typeof(BoxCollider))]
    public class GoalPortal : MonoBehaviour
    {
        public float spinSpeed = 45f;

        void Awake()
        {
            var col = GetComponent<BoxCollider>();
            col.isTrigger = true;
        }

        void Update()
        {
            transform.Rotate(Vector3.up, spinSpeed * Time.deltaTime, Space.World);
        }

        void OnTriggerEnter(Collider other)
        {
            if (!other.CompareTag("Player") && other.GetComponentInParent<AdventurePlayerController>() == null) return;
            if (AdventureGameManager.Instance != null) AdventureGameManager.Instance.OnGoalReached();
        }
    }
}
