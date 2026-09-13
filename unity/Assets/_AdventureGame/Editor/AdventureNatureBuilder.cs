using UnityEngine;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine.Rendering;
using System.Collections.Generic;

namespace AdventureGame
{
    /// <summary>
    /// Scatters the imported nature model pack (Common/Pine/Twisted/Dead trees,
    /// bushes, grass, flowers, clover, ferns, plants, mushrooms, pebbles, rocks
    /// and rock-path pieces) around the play area. Gameplay spots (spawn, NPC,
    /// shards, enemies, goal, pond, trail) are kept clear. Mesh vertex colors
    /// are baked into cached URP materials per submesh, so trunks stay brown
    /// and leaves stay green with zero manual setup.
    /// Menu: Adventure &gt; Apply Nature World (also automatic in Build).
    /// </summary>
    public static class AdventureNatureBuilder
    {
        const string NatureDir = "Assets/_AdventureGame/Nature/FBX (Unity)";

        static readonly Dictionary<string, Material> vcMats = new Dictionary<string, Material>();
        static readonly System.Random rng = new System.Random(12345);

        // (x, z, radius) keep-clear zones on the ground plane.
        static readonly Vector3[] clearSpots =
        {
            new Vector3(0f, 0f, 3f),      // spawn
            new Vector3(4f, 4f, 2.5f),    // Y-Bot NPC
            new Vector3(12f, 12f, 3f),    // goal
            new Vector3(-6f, 6f, 1.6f), new Vector3(-2f, -6f, 1.6f), new Vector3(6f, -2f, 1.6f),
            new Vector3(3f, -2f, 1.6f), new Vector3(9f, 8f, 1.6f),   // shards
            new Vector3(-7f, -5f, 2f), new Vector3(7f, 7f, 2f),      // enemies
            new Vector3(-9f, -9f, 4.2f),  // pond
            new Vector3(-11f, 11f, 3.5f), // boss arena
        };

        static readonly Vector2 pathA = new Vector2(0f, 0f);
        static readonly Vector2 pathB = new Vector2(12f, 12f);

        [MenuItem("Adventure/Apply Nature World")]
        public static void ApplyFromMenu()
        {
            if (EditorApplication.isPlaying)
            {
                EditorUtility.DisplayDialog("Adventure", "Exit Play mode first, then apply again.", "OK");
                return;
            }
            AdventureArtGenerator.EnsureArt();
            var root = GameObject.Find("Adventure");
            if (root == null)
            {
                Debug.LogError("[Adventure] Run Adventure > Build Prototype Level first.");
                return;
            }
            AdventureWorldBuilder.ApplySkyAndLight();
            var worldT = root.transform.Find("World");
            if (worldT == null)
            {
                Debug.LogError("[Adventure] No World node found; rebuild the level.");
                return;
            }
            EnsureNature(worldT.gameObject);
            EditorSceneManager.MarkSceneDirty(EditorSceneManager.GetActiveScene());
            Debug.Log("[Adventure] Nature World applied. Press Play.");
        }

        /// <summary>Idempotent: replaces legacy primitive decor once, then refreshes colors.</summary>
        public static void EnsureNature(GameObject world)
        {
            if (world.transform.Find("NatureScatter") == null)
            {
                var doomed = new List<GameObject>();
                foreach (Transform child in world.transform)
                    if (child.name == "Pine" || child.name == "Rock" || child.name == "Flower")
                        doomed.Add(child.gameObject);
                foreach (var d in doomed) Object.DestroyImmediate(d);
                ScatterNature(world);
            }
            RefreshNatureMaterials(world);
        }

        /// <summary>Re-applies slot colors to the already-scattered models (fixes white trees in place).</summary>
        public static void RefreshNatureMaterials(GameObject world)
        {
            EnsureMaterialImport();
            var scatter = world.transform.Find("NatureScatter");
            if (scatter == null) return;
            int fixedCount = 0;
            foreach (Transform child in scatter.transform)
            {
                ApplyNatureMaterials(child.gameObject, child.name);
                fixedCount++;
            }
            Debug.Log($"[Adventure] Refreshed materials on {fixedCount} nature objects.");
        }

        public static void ScatterNature(GameObject world)
        {
            EditorUtility.DisplayProgressBar("Adventure", "Preparing nature pack (first run reimports models)...", 0.1f);
            try
            {
                EnsureReadableModels();
                EnsureMaterialImport();
                var scatter = new GameObject("NatureScatter");
                scatter.transform.SetParent(world.transform, false);
                Undo.RegisterCreatedObjectUndo(scatter, "Nature scatter");

            // --- statement trees (solid obstacles) ---
            PlaceAt(scatter, "CommonTree_1", new Vector3(-16f, 0f, -14f), 1.2f, true);
            PlaceAt(scatter, "Pine_2", new Vector3(16f, 0f, -16f), 1.3f, true);
            PlaceAt(scatter, "TwistedTree_1", new Vector3(-18f, 0f, 2f), 1.1f, true);
            PlaceAt(scatter, "CommonTree_3", new Vector3(18f, 0f, 4f), 1.2f, true);
            PlaceAt(scatter, "Pine_4", new Vector3(-14f, 0f, 14f), 1.3f, true);
            PlaceAt(scatter, "CommonTree_5", new Vector3(14f, 0f, -10f), 1.1f, true);
            PlaceAt(scatter, "TwistedTree_2", new Vector3(-8f, 0f, -17f), 1.0f, true);
            PlaceAt(scatter, "Pine_1", new Vector3(8f, 0f, 17f), 1.2f, true);
            PlaceAt(scatter, "CommonTree_2", new Vector3(-2f, 0f, 14f), 1.0f, true);
            PlaceAt(scatter, "Pine_5", new Vector3(-4f, 0f, -14f), 1.1f, true);
            // --- dead-tree corner (spooky west side) ---
            PlaceAt(scatter, "DeadTree_1", new Vector3(-15f, 0f, 6f), 1.1f, true);
            PlaceAt(scatter, "DeadTree_2", new Vector3(-16.5f, 0f, 8.5f), 1.0f, true);
            PlaceAt(scatter, "DeadTree_3", new Vector3(-13.5f, 0f, 10.5f), 1.2f, true);

            // --- rock trail from spawn to goal ---
            string[] pathPieces = { "RockPath_Round_Small_1", "RockPath_Square_Small_2", "RockPath_Round_Small_3", "RockPath_Square_Small_1" };
            for (int i = 0; i < 11; i++)
            {
                float t = 0.06f + i * (0.88f / 10f);
                Vector3 p = Vector3.Lerp(new Vector3(0f, 0.03f, 0f), new Vector3(12f, 0.03f, 12f), t);
                PlaceAt(scatter, pathPieces[i % pathPieces.Length], p, 0.9f + (float)rng.NextDouble() * 0.25f, false, 45f, false);
            }
            PlaceAt(scatter, "RockPath_Round_Wide", new Vector3(0f, 0.03f, 0f), 1f, false, 45f, false);
            PlaceAt(scatter, "RockPath_Square_Wide", new Vector3(12f, 0.03f, 12f), 1f, false, 45f, false);

            // --- clover + fern ring around the pond ---
            for (int i = 0; i < 8; i++)
            {
                float a = i * Mathf.PI * 2f / 8f + 0.3f;
                Vector3 p = new Vector3(-9f + Mathf.Cos(a) * 4.6f, 0f, -9f + Mathf.Sin(a) * 4.6f);
                PlaceAt(scatter, i % 2 == 0 ? "Clover_1" : "Fern_1", p, 0.9f + (float)rng.NextDouble() * 0.4f, false, -1f, false);
            }

            // --- mushrooms near dead trees and a pine ---
            PlaceAt(scatter, "Mushroom_Common", new Vector3(-14f, 0f, 7f), 1f, false, -1f, false);
            PlaceAt(scatter, "Mushroom_Laetiporus", new Vector3(-16f, 0f, 9.5f), 1f, false, -1f, false);
            PlaceAt(scatter, "Mushroom_Common", new Vector3(-13f, 0f, 9f), 1.2f, false, -1f, false);
            PlaceAt(scatter, "Mushroom_Laetiporus", new Vector3(15f, 0f, -15f), 1f, false, -1f, false);
            PlaceAt(scatter, "Mushroom_Common", new Vector3(17f, 0f, -15f), 0.9f, false, -1f, false);
            PlaceAt(scatter, "Mushroom_Common", new Vector3(17f, 0f, 3f), 1f, false, -1f, false);

            // --- random scatter ---
            ScatterRandom(scatter, new[] { "Bush_Common", "Bush_Common_Flowers" }, 8, 0.8f, 1.2f, true, false);
            ScatterRandom(scatter, new[] { "Rock_Medium_1", "Rock_Medium_2", "Rock_Medium_3" }, 7, 0.7f, 1.3f, true, false);
            ScatterRandom(scatter, new[] { "Grass_Common_Short", "Grass_Common_Tall", "Grass_Wispy_Short", "Grass_Wispy_Tall" }, 30, 0.8f, 1.4f, false, true, false);
            ScatterRandom(scatter, new[] { "Flower_3_Single", "Flower_3_Group", "Flower_4_Single", "Flower_4_Group" }, 12, 0.8f, 1.2f, false, false, false);
            ScatterRandom(scatter, new[] { "Plant_1", "Plant_1_Big", "Plant_7", "Plant_7_Big" }, 8, 0.8f, 1.3f, false, false, false);
            ScatterRandom(scatter, new[] { "Pebble_Round_1", "Pebble_Round_3", "Pebble_Round_5", "Pebble_Square_2", "Pebble_Square_4", "Pebble_Square_6" }, 14, 0.7f, 1.4f, false, true, false);
            ScatterRandom(scatter, new[] { "Petal_1", "Petal_3", "Petal_5" }, 6, 0.9f, 1.1f, false, false, false);

                EditorUtility.DisplayProgressBar("Adventure", "Nature scatter done.", 1f);
                Debug.Log($"[Adventure] Scattered {scatter.transform.childCount} nature objects under World/NatureScatter.");
            }
            finally
            {
                EditorUtility.ClearProgressBar();
            }
        }

        // ---------- placement ----------

        static void ScatterRandom(GameObject parent, string[] fbxs, int count, float sMin, float sMax, bool collider, bool allowOnPath, bool castShadow = true)
        {
            int placed = 0, tries = 0;
            while (placed < count && tries < count * 25)
            {
                tries++;
                Vector3 p = new Vector3((float)(rng.NextDouble() * 38.0 - 19.0), 0f, (float)(rng.NextDouble() * 38.0 - 19.0));
                if (!IsClear(p, allowOnPath)) continue;
                string fbx = fbxs[rng.Next(fbxs.Length)];
                float s = sMin + (float)rng.NextDouble() * (sMax - sMin);
                if (PlaceAt(parent, fbx, p, s, collider, -1f, castShadow)) placed++;
            }
        }

        static bool IsClear(Vector3 p, bool allowOnPath)
        {
            foreach (var c in clearSpots)
            {
                float dx = p.x - c.x, dz = p.z - c.y;
                if (dx * dx + dz * dz < c.z * c.z) return false;
            }
            if (!allowOnPath && DistToPath(p) < 1.5f) return false;
            return true;
        }

        static float DistToPath(Vector3 p)
        {
            Vector2 q = new Vector2(p.x, p.z);
            Vector2 ab = pathB - pathA;
            float t = Mathf.Clamp01(Vector2.Dot(q - pathA, ab) / ab.sqrMagnitude);
            return Vector2.Distance(q, pathA + ab * t);
        }

        static bool PlaceAt(GameObject parent, string fbx, Vector3 pos, float scale, bool collider, float yaw = -1f, bool castShadow = true)
        {
            var prefab = AssetDatabase.LoadAssetAtPath<GameObject>($"{NatureDir}/{fbx}.fbx");
            if (prefab == null)
            {
                Debug.LogWarning($"[Adventure] Missing nature asset: {fbx}");
                return false;
            }
            var go = (GameObject)PrefabUtility.InstantiatePrefab(prefab);
            go.name = fbx;
            go.transform.SetParent(parent.transform, false);
            go.transform.position = pos;
            go.transform.localScale = Vector3.one * scale;
            go.transform.rotation = Quaternion.Euler(0f, yaw >= 0f ? yaw : (float)rng.NextDouble() * 360f, 0f);
            ApplyNatureMaterials(go, fbx);
            if (collider) AddStaticColliders(go);
            if (!castShadow)
                foreach (var r in go.GetComponentsInChildren<Renderer>())
                    r.shadowCastingMode = ShadowCastingMode.Off;
            go.isStatic = true; // static batching for all placed nature
            return true;
        }

        // ---------- materials by slot name ----------

        // The pack's vertex colors are white placeholders; the real color info
        // is the FBX material slot names (Bark_*, Leaves_*, Rocks, ...).
        // Unity must import those slots, then we paint each by keyword.
        static void EnsureMaterialImport()
        {
            foreach (var guid in AssetDatabase.FindAssets("t:Model", new[] { "Assets/_AdventureGame/Nature" }))
            {
                string path = AssetDatabase.GUIDToAssetPath(guid);
                var importer = AssetImporter.GetAtPath(path) as ModelImporter;
                if (importer == null) continue;
                if (importer.materialImportMode != ModelImporterMaterialImportMode.ImportViaMaterialDescription)
                {
                    importer.materialImportMode = ModelImporterMaterialImportMode.ImportViaMaterialDescription;
                    importer.SaveAndReimport();
                    Debug.Log("[Adventure] Material import enabled: " + path);
                }
            }
        }

        static void ApplyNatureMaterials(GameObject go, string fbx)
        {
            var prefab = AssetDatabase.LoadAssetAtPath<GameObject>($"{NatureDir}/{fbx}.fbx");
            if (prefab == null) return;
            var srcRenderers = prefab.GetComponentsInChildren<MeshRenderer>();
            var dstRenderers = go.GetComponentsInChildren<MeshRenderer>();
            int n = Mathf.Min(srcRenderers.Length, dstRenderers.Length);
            for (int j = 0; j < n; j++)
            {
                var srcSlots = srcRenderers[j].sharedMaterials;
                var dstSlots = dstRenderers[j].sharedMaterials;
                int m = Mathf.Min(srcSlots.Length, dstSlots.Length);
                for (int i = 0; i < m; i++)
                {
                    string slot = srcSlots[i] != null ? srcSlots[i].name : "";
                    Color? mapped = MapSlotColor(slot);
                    if (mapped.HasValue) dstSlots[i] = GetVCMaterial(mapped.Value);
                }
                dstRenderers[j].sharedMaterials = dstSlots;
            }
            // Defensive: pack uses regular meshes, but cover skinned too.
            var srcSkinned = prefab.GetComponentsInChildren<SkinnedMeshRenderer>();
            var dstSkinned = go.GetComponentsInChildren<SkinnedMeshRenderer>();
            int sn = Mathf.Min(srcSkinned.Length, dstSkinned.Length);
            for (int j = 0; j < sn; j++)
            {
                var srcSlots = srcSkinned[j].sharedMaterials;
                var dstSlots = dstSkinned[j].sharedMaterials;
                int m = Mathf.Min(srcSlots.Length, dstSlots.Length);
                for (int i = 0; i < m; i++)
                {
                    string slot = srcSlots[i] != null ? srcSlots[i].name : "";
                    Color? mapped = MapSlotColor(slot);
                    if (mapped.HasValue) dstSlots[i] = GetVCMaterial(mapped.Value);
                }
                dstSkinned[j].sharedMaterials = dstSlots;
            }
        }

        static Color? MapSlotColor(string slotName)
        {
            string n = (slotName ?? "").ToLowerInvariant();
            if (n.Contains("bark") || n.Contains("trunk") || n.Contains("wood") || n.Contains("branch") || n == "material")
                return new Color(0.42f, 0.30f, 0.18f);
            if (n.Contains("pine") || n.Contains("needle"))
                return new Color(0.16f, 0.42f, 0.20f);
            if (n.Contains("leaves") || n.Contains("leaf") || n.Contains("grass") || n.Contains("fern") ||
                n.Contains("clover") || n.Contains("bush") || n.Contains("plant") || n.Contains("moss"))
                return new Color(0.30f, 0.55f, 0.26f);
            if (n.Contains("rock") || n.Contains("stone") || n.Contains("pebble"))
                return new Color(0.50f, 0.50f, 0.53f);
            if (n.Contains("flower") || n.Contains("petal") || n.Contains("blossom"))
                return new Color(1f, 0.45f, 0.65f);
            if (n.Contains("mushroom") || n.Contains("cap"))
                return new Color(0.80f, 0.25f, 0.20f);
            if (n.Contains("stem") || n.Contains("stalk"))
                return new Color(0.90f, 0.85f, 0.75f);
            return null; // unknown slot: keep the imported material
        }

        public static Material GetVCMaterial(Color c)
        {
            string key = $"{Mathf.RoundToInt(c.r * 32)}_{Mathf.RoundToInt(c.g * 32)}_{Mathf.RoundToInt(c.b * 32)}";
            if (vcMats.TryGetValue(key, out var m) && m != null) return m;
            var shader = Shader.Find("Universal Render Pipeline/Lit") ?? Shader.Find("Standard");
            m = new Material(shader);
            Color col = c;
            col.a = 1f;
            if (m.HasProperty("_BaseColor")) m.SetColor("_BaseColor", col);
            else if (m.HasProperty("_Color")) m.color = col;
            m.name = "M_NatureVC_" + key;
            vcMats[key] = m;
            return m;
        }

        static void AddStaticColliders(GameObject go)
        {
            foreach (var mf in go.GetComponentsInChildren<MeshFilter>())
            {
                if (mf.GetComponent<Collider>() != null) continue;
                var mc = mf.gameObject.AddComponent<MeshCollider>();
                mc.sharedMesh = mf.sharedMesh;
            }
        }

        // Vertex colors need Read/Write enabled; flip it on once for the pack.
        static void EnsureReadableModels()
        {
            bool touched = false;
            foreach (var guid in AssetDatabase.FindAssets("t:Model", new[] { "Assets/_AdventureGame/Nature" }))
            {
                string path = AssetDatabase.GUIDToAssetPath(guid);
                var importer = AssetImporter.GetAtPath(path) as ModelImporter;
                if (importer != null && !importer.isReadable)
                {
                    importer.isReadable = true;
                    importer.SaveAndReimport();
                    touched = true;
                }
            }
            if (touched) AssetDatabase.Refresh();
        }
    }
}
