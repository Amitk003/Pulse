using UnityEngine;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine.Rendering;

namespace AdventureGame
{
    /// <summary>
    /// Meadow Valley theme: procedural sky + fog, textured ground/path/walls,
    /// pond, pine trees, rocks, flowers and drifting clouds.
    /// Menu: Adventure &gt; Apply Meadow World (also runs at the end of Build).
    /// Re-running is safe: materials/sky re-apply, decor builds once.
    /// </summary>
    public static class AdventureWorldBuilder
    {
        [MenuItem("Adventure/Apply Meadow World")]
        public static void ApplyFromMenu()
        {
            if (EditorApplication.isPlaying)
            {
                EditorUtility.DisplayDialog("Adventure", "Exit Play mode first, then apply again.", "OK");
                return;
            }
            ApplyMeadowWorld();
            EditorSceneManager.MarkSceneDirty(EditorSceneManager.GetActiveScene());
            Debug.Log("[Adventure] Meadow World applied. Press Play.");
        }

        public static void ApplyMeadowWorld()
        {
            AdventureArtGenerator.EnsureArt();
            var root = GameObject.Find("Adventure");
            if (root == null)
            {
                Debug.LogError("[Adventure] Build the Prototype Level first (Adventure > Build Prototype Level).");
                return;
            }
            ApplySkyAndLight();
            ApplyMaterials(root);
            BuildDecor(root);
        }

        // ---------- sky / light / fog ----------

        public static void ApplySkyAndLight()
        {
            var sky = AdventureArtGenerator.LoadMat("Sky_Meadow");
            if (sky != null) RenderSettings.skybox = sky;
            RenderSettings.fog = true;
            RenderSettings.fogColor = new Color(0.70f, 0.83f, 0.95f);
            RenderSettings.fogMode = FogMode.Linear;
            RenderSettings.fogStartDistance = 30f;
            RenderSettings.fogEndDistance = 95f;
            RenderSettings.ambientMode = AmbientMode.Skybox;
            RenderSettings.ambientIntensity = 1f;

            var sun = Object.FindAnyObjectByType<Light>(FindObjectsInactive.Exclude);
            if (sun != null && sun.type == LightType.Directional)
            {
                sun.color = new Color(1f, 0.96f, 0.88f);
                sun.intensity = 2f;
            }
        }

        // ---------- re-texture the blockout ----------

        static void ApplyMaterials(GameObject root)
        {
            SetMat(root, "Environment/Ground", "M_Grass");
            SetMat(root, "Environment/Wall_N", "M_Rock");
            SetMat(root, "Environment/Wall_S", "M_Rock");
            SetMat(root, "Environment/Wall_E", "M_Rock");
            SetMat(root, "Environment/Wall_W", "M_Rock");
            SetMat(root, "Environment/Platform_A", "M_Wood");
            SetMat(root, "Environment/Platform_B", "M_Wood");
            SetMat(root, "Environment/Stair_0", "M_Wood");
            SetMat(root, "Environment/Stair_1", "M_Wood");
            SetMat(root, "Environment/Stair_2", "M_Wood");
        }

        static void SetMat(GameObject root, string childPath, string matName)
        {
            var t = root.transform.Find(childPath);
            if (t == null) return;
            var r = t.GetComponent<Renderer>();
            var m = AdventureArtGenerator.LoadMat(matName);
            if (r != null && m != null) r.sharedMaterial = m;
        }

        // ---------- decor ----------

        static void BuildDecor(GameObject root)
        {
            var existing = root.transform.Find("World");
            if (existing != null)
            {
                // Upgrade pass: swap legacy primitive decor for the nature pack.
                AdventureNatureBuilder.EnsureNature(existing.gameObject);
                return;
            }
            var world = new GameObject("World");
            world.transform.SetParent(root.transform, false);
            Undo.RegisterCreatedObjectUndo(world, "Meadow World");

            BuildPath(world);
            BuildPond(world, new Vector3(-9f, 0f, -9f), 3f);
            AdventureNatureBuilder.EnsureNature(world);

            BuildCloud(world, new Vector3(-10f, 13f, -6f), new Vector3(5f, 1.4f, 3f));
            BuildCloud(world, new Vector3(6f, 14f, 8f), new Vector3(6f, 1.6f, 3.5f));
            BuildCloud(world, new Vector3(0f, 15f, -14f), new Vector3(4f, 1.2f, 2.5f));
            BuildCloud(world, new Vector3(12f, 12f, 0f), new Vector3(4.5f, 1.3f, 2.8f));
        }

        static void BuildPath(GameObject world)
        {
            // Dirt trail from spawn (0,0) to the goal gate (12,12).
            var go = GameObject.CreatePrimitive(PrimitiveType.Plane);
            go.name = "DirtPath";
            go.transform.SetParent(world.transform, false);
            go.transform.position = new Vector3(6f, 0.02f, 6f);
            go.transform.rotation = Quaternion.Euler(0f, 45f, 0f);
            go.transform.localScale = new Vector3(0.22f, 1f, 1.9f); // plane is 10x10 units
            var meshCol = go.GetComponent<MeshCollider>();
            if (meshCol != null) Object.DestroyImmediate(meshCol);
            var m = AdventureArtGenerator.LoadMat("M_Path");
            if (m != null) go.GetComponent<Renderer>().sharedMaterial = m;
            go.GetComponent<Renderer>().shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            go.isStatic = true;
        }

        static void BuildPond(GameObject world, Vector3 center, float radius)
        {
            var go = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            go.name = "Pond";
            go.transform.SetParent(world.transform, false);
            go.transform.position = center + Vector3.up * 0.05f;
            go.transform.localScale = new Vector3(radius * 2f, 0.05f, radius * 2f);
            var col = go.GetComponent<CapsuleCollider>();
            if (col != null) Object.DestroyImmediate(col);
            var m = AdventureArtGenerator.LoadMat("M_Water");
            if (m != null) go.GetComponent<Renderer>().sharedMaterial = m;
            go.GetComponent<Renderer>().shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            go.isStatic = true;
            // Sandy rim: flat dirt ring under the water edge.
            var rim = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            rim.name = "PondRim";
            rim.transform.SetParent(world.transform, false);
            rim.transform.position = center + Vector3.up * 0.02f;
            rim.transform.localScale = new Vector3(radius * 2f + 1f, 0.04f, radius * 2f + 1f);
            var rimCol = rim.GetComponent<CapsuleCollider>();
            if (rimCol != null) Object.DestroyImmediate(rimCol);
            var dirt = AdventureArtGenerator.LoadMat("M_Dirt");
            if (dirt != null) rim.GetComponent<Renderer>().sharedMaterial = dirt;
            rim.GetComponent<Renderer>().shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            rim.isStatic = true;
        }

        static void BuildCloud(GameObject world, Vector3 pos, Vector3 size)
        {
            var go = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            go.name = "Cloud";
            go.transform.SetParent(world.transform, false);
            go.transform.position = pos;
            go.transform.localScale = size;
            var col = go.GetComponent<SphereCollider>();
            if (col != null) Object.DestroyImmediate(col);
            var m = AdventureArtGenerator.LoadMat("M_Cloud");
            if (m != null) go.GetComponent<Renderer>().sharedMaterial = m;
            go.GetComponent<Renderer>().shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            go.AddComponent<CloudDrift>();
        }

    }
}
