using UnityEngine;

namespace AdventureGame
{
    /// <summary>
    /// Slow horizontal drift for stylized clouds. Attach to a cloud puff;
    /// it ping-pongs around its start position. Added by the world builder.
    /// </summary>
    public class CloudDrift : MonoBehaviour
    {
        public float speed = 0.35f;
        public float range = 3f;

        Vector3 start;
        float phase;

        void Awake()
        {
            start = transform.position;
            phase = Random.Range(0f, 100f);
        }

        void Update()
        {
            float x = Mathf.Sin(Time.time * speed + phase) * range;
            transform.position = start + new Vector3(x, 0f, 0f);
        }
    }
}
