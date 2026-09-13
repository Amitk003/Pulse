using UnityEngine;

namespace AdventureGame
{
    /// <summary>
    /// Procedural juice: spark bursts, expanding shockwave rings and a light
    /// flash. No textures or prefabs needed. Everything self-destroys.
    /// </summary>
    public static class HitBurst
    {
        static Material sparkMat;

        public static void Spawn(Vector3 pos, Color color, float size = 1f, int count = 6)
        {
            if (sparkMat == null)
            {
                var shader = Shader.Find("Universal Render Pipeline/Unlit") ?? Shader.Find("Sprites/Default");
                sparkMat = new Material(shader);
                if (sparkMat.HasProperty("_BaseColor")) sparkMat.SetColor("_BaseColor", Color.white);
                else if (sparkMat.HasProperty("_Color")) sparkMat.color = Color.white;
            }
            for (int i = 0; i < count; i++)
            {
                var go = GameObject.CreatePrimitive(PrimitiveType.Sphere);
                var col = go.GetComponent<SphereCollider>();
                if (col != null) Object.Destroy(col);
                go.transform.position = pos;
                go.transform.localScale = Vector3.one * (0.1f * size);
                var bitMat = new Material(sparkMat);
                if (bitMat.HasProperty("_BaseColor")) bitMat.SetColor("_BaseColor", color);
                else if (bitMat.HasProperty("_Color")) bitMat.color = color;
                go.GetComponent<Renderer>().sharedMaterial = bitMat;
                var bit = go.AddComponent<SparkBit>();
                bit.velocity = Random.onUnitSphere * Random.Range(3f, 6.5f) * size;
                bit.velocity.y = Mathf.Abs(bit.velocity.y) + 2f;
                bit.life = 0.35f;
            }
            var flash = new GameObject("HitFlash");
            var light = flash.AddComponent<Light>();
            light.type = LightType.Point;
            light.color = color;
            light.intensity = 3f;
            light.range = 6f * size;
            flash.transform.position = pos;
            Object.Destroy(flash, 0.12f);
        }

        public static void Shockwave(Vector3 pos, Color color, float maxRadius = 3f)
        {
            var go = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            var col = go.GetComponent<CapsuleCollider>();
            if (col != null) Object.Destroy(col);
            go.name = "ShockRing";
            go.transform.position = pos + Vector3.up * 0.15f;
            go.transform.localScale = new Vector3(0.5f, 0.08f, 0.5f);
            var shader = Shader.Find("Universal Render Pipeline/Unlit") ?? Shader.Find("Sprites/Default");
            var mat = new Material(shader);
            var c = color; c.a = 0.7f;
            if (mat.HasProperty("_BaseColor")) mat.SetColor("_BaseColor", c);
            else if (mat.HasProperty("_Color")) mat.color = c;
            if (mat.HasProperty("_Surface"))
            {
                mat.SetFloat("_Surface", 1f);
                mat.SetFloat("_Blend", 0f);
                mat.SetOverrideTag("RenderType", "Transparent");
                mat.renderQueue = 3000;
            }
            go.GetComponent<Renderer>().sharedMaterial = mat;
            go.GetComponent<Renderer>().shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            var ring = go.AddComponent<ShockRing>();
            ring.mat = mat;
            ring.maxRadius = maxRadius;
        }

        class SparkBit : MonoBehaviour
        {
            public Vector3 velocity;
            public float life = 0.35f;
            float age;

            void Update()
            {
                age += Time.deltaTime;
                if (age >= life) { Destroy(gameObject); return; }
                velocity.y -= 12f * Time.deltaTime;
                transform.position += velocity * Time.deltaTime;
                float s = 1f - age / life;
                transform.localScale = Vector3.one * (0.1f * s + 0.01f);
            }

            void OnDestroy()
            {
                var r = GetComponent<Renderer>();
                if (r != null && r.sharedMaterial != null) Destroy(r.sharedMaterial);
            }
        }

        class ShockRing : MonoBehaviour
        {
            public Material mat;
            public float maxRadius = 3f;
            float age;
            const float Duration = 0.35f;

            void Update()
            {
                age += Time.deltaTime;
                float t = Mathf.Clamp01(age / Duration);
                float r = Mathf.Lerp(0.5f, maxRadius, t);
                transform.localScale = new Vector3(r, 0.08f, r);
                if (mat != null)
                {
                    Color c = mat.HasProperty("_BaseColor") ? mat.GetColor("_BaseColor") : Color.white;
                    c.a = 0.7f * (1f - t);
                    if (mat.HasProperty("_BaseColor")) mat.SetColor("_BaseColor", c);
                    else if (mat.HasProperty("_Color")) mat.color = c;
                }
                if (t >= 1f) Destroy(gameObject);
            }

            void OnDestroy()
            {
                if (mat != null) Destroy(mat);
            }
        }
    }
}
