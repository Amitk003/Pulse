using UnityEngine;

namespace AdventureGame
{
    /// <summary>
    /// Simple melee enemy: patrols waypoints, chases the Player tag in range,
    /// attacks for damage, dies to AdventurePlayerController.TryAttack().
    /// No NavMesh needed.
    /// </summary>
    [RequireComponent(typeof(CharacterController))]
    public class EnemyChaser : MonoBehaviour
    {
        [Header("Stats")]
        public int maxHealth = 30;
        public int attackDamage = 10;
        public float patrolSpeed = 2f;
        public float chaseSpeed = 3.5f;
        public float chaseRange = 10f;
        public float attackRange = 1.9f;
        public float attackCooldown = 1.2f;
        public float gravity = 20f;

        [Header("Patrol")]
        public Vector3[] patrolOffsets = new Vector3[]
        {
            new Vector3(-4f, 0f, 0f),
            new Vector3(4f, 0f, 0f),
        };

        protected CharacterController cc;
        protected Transform player;
        protected int health;
        protected float attackTimer;
        protected float verticalVel;
        protected Vector3 knockback;
        protected float knockbackPower = 4.5f;
        protected float flinch;
        protected Vector3 home;
        protected Renderer[] renderers;
        int patrolIndex;
        float flashTimer;

        protected virtual void Awake()
        {
            cc = GetComponent<CharacterController>();
            home = transform.position;
            health = maxHealth;
            renderers = GetComponentsInChildren<Renderer>();
        }

        protected virtual void Update()
        {
            if (AdventureGameManager.Instance != null && AdventureGameManager.Instance.gameOver) return;
            if (player == null)
            {
                var p = GameObject.FindGameObjectWithTag("Player");
                if (p != null) player = p.transform;
            }

            attackTimer -= Time.deltaTime;
            if (flashTimer > 0f)
            {
                flashTimer -= Time.deltaTime;
                if (flashTimer <= 0f) SetTint(Color.red);
            }

            Vector3 move = Vector3.zero;
            bool chasing = false;
            if (player != null)
            {
                float d = Vector3.Distance(transform.position, player.position);
                if (d <= chaseRange)
                {
                    chasing = true;
                    Vector3 to = player.position - transform.position;
                    to.y = 0f;
                    if (to.sqrMagnitude > 0.01f)
                    {
                        transform.rotation = Quaternion.Slerp(transform.rotation,
                            Quaternion.LookRotation(to.normalized), Time.deltaTime * 8f);
                        if (d > attackRange) move = to.normalized * chaseSpeed;
                        else if (attackTimer <= 0f)
                        {
                            attackTimer = attackCooldown;
                            if (AdventureGameManager.Instance != null)
                                AdventureGameManager.Instance.DamagePlayer(attackDamage);
                        }
                    }
                }
            }

            if (!chasing)
            {
                Vector3 dest = home + patrolOffsets[patrolIndex % patrolOffsets.Length];
                dest.y = transform.position.y;
                Vector3 to = dest - transform.position;
                to.y = 0f;
                if (to.magnitude < 0.5f) patrolIndex++;
                else
                {
                    transform.rotation = Quaternion.Slerp(transform.rotation,
                        Quaternion.LookRotation(to.normalized), Time.deltaTime * 5f);
                    move = to.normalized * patrolSpeed;
                }
            }

            if (cc.isGrounded && verticalVel < 0f) verticalVel = -2f;
            verticalVel -= gravity * Time.deltaTime;
            move += knockback;
            knockback = Vector3.Lerp(knockback, Vector3.zero, Time.deltaTime * 8f);
            move.y = verticalVel;
            cc.Move(move * Time.deltaTime);

            // Procedural recoil: squash-and-stretch snap on every hit.
            if (flinch > 0f) flinch = Mathf.Max(0f, flinch - Time.deltaTime * 6f);
            float f = flinch;
            transform.localScale = new Vector3(1f - 0.12f * f, 1f + 0.18f * f, 1f - 0.12f * f);
        }

        public virtual void TakeDamage(int amount, Vector3 hitFrom = default)
        {
            if (health <= 0) return;
            health -= amount;
            Vector3 push = transform.position - hitFrom;
            push.y = 0f;
            if (push.sqrMagnitude > 0.01f) knockback = push.normalized * knockbackPower;
            OnTookHit();
            if (health <= 0) Die();
        }

        // Hit flash. Boss overrides this: tinting would wreck its cel materials.
        protected virtual void OnTookHit()
        {
            flashTimer = 0.12f;
            SetTint(Color.white);
        }

        void SetTint(Color c)
        {
            foreach (var r in renderers)
            {
                if (r == null || r.material == null) continue;
                var m = r.material;
                // Flash to white, then restore the ORIGINAL color (captured on
                // first tint) — otherwise the red never fully comes back.
                bool flashing = c == Color.white && flashTimer > 0f;
                if (flashing && !originalTints.ContainsKey(r))
                    originalTints[r] = m.HasProperty("_BaseColor") ? m.GetColor("_BaseColor")
                                        : m.HasProperty("_Color") ? m.GetColor("_Color") : Color.white;
                if (!flashing && originalTints.ContainsKey(r)) c = originalTints[r];
                if (m.HasProperty("_BaseColor")) m.SetColor("_BaseColor", c);
                else if (m.HasProperty("_Color")) m.color = c;
            }
        }

        readonly System.Collections.Generic.Dictionary<Renderer, Color> originalTints = new();

        protected virtual void Die()
        {
            if (AdventureGameManager.Instance != null) AdventureGameManager.Instance.OnEnemyDefeated(this);
            Destroy(gameObject);
        }

        void OnDrawGizmosSelected()
        {
            Gizmos.color = Color.yellow;
            Gizmos.DrawWireSphere(transform.position, chaseRange);
            Gizmos.color = Color.red;
            Gizmos.DrawWireSphere(transform.position, attackRange);
        }
    }
}
