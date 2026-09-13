using UnityEngine;

namespace AdventureGame
{
    /// <summary>
    /// Spinning pickup shard. Builder adds a trigger SphereCollider.
    /// </summary>
    [RequireComponent(typeof(SphereCollider))]
    public class Collectible : MonoBehaviour
    {
        public int scoreValue = 1;
        public int healAmount;
        public float spinSpeed = 90f;
        public float bobAmplitude = 0.25f;
        public float bobFrequency = 2f;

        Vector3 basePos;
        bool taken;

        void Awake()
        {
            var col = GetComponent<SphereCollider>();
            col.isTrigger = true;
            col.radius = 1f;
            basePos = transform.position;
        }

        void Update()
        {
            transform.Rotate(Vector3.up, spinSpeed * Time.deltaTime, Space.World);
            transform.position = basePos + Vector3.up * (Mathf.Sin(Time.time * bobFrequency) * bobAmplitude);
        }

        void OnTriggerEnter(Collider other)
        {
            if (taken) return;
            if (!other.CompareTag("Player") && other.GetComponentInParent<AdventurePlayerController>() == null) return;
            taken = true;
            HitBurst.Spawn(transform.position, new Color(1f, 0.9f, 0.3f), 1f, 8);
            if (AdventureGameManager.Instance != null) AdventureGameManager.Instance.OnCollectiblePicked(this);
            Destroy(gameObject);
        }
    }
}
