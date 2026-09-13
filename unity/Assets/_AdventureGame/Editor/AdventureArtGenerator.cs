using UnityEngine;
using UnityEditor;
using System.IO;

namespace AdventureGame
{
    /// <summary>
    /// Procedurally generates the Meadow World art: tiling PNG textures
    /// (grass, dirt, rock, wood) plus URP materials and a skybox.
    /// Menu: Adventure &gt; Generate World Art. Safe to re-run (skips existing).
    /// No external assets needed.
    /// </summary>
    public static class AdventureArtGenerator
    {
        public const string ArtDir = "Assets/_AdventureGame/Art";
        public const string TexDir = ArtDir + "/Textures";
        public const string MatDir = ArtDir + "/Materials";

        [MenuItem("Adventure/Generate World Art")]
        public static void Generate()
        {
            if (EditorApplication.isPlaying)
            {
                EditorUtility.DisplayDialog("Adventure", "Exit Play mode first, then generate again.", "OK");
                return;
            }
            EnsureArt();
            AssetDatabase.Refresh();
            Debug.Log("[Adventure] World art ready in " + ArtDir);
        }

        public static void EnsureArt()
        {
            Directory.CreateDirectory(Application.dataPath + "/_AdventureGame/Art/Textures");
            Directory.CreateDirectory(Application.dataPath + "/_AdventureGame/Art/Materials");
            AssetDatabase.Refresh();

            SaveTexture("T_Grass", 256, GrassPixel);
            SaveTexture("T_Dirt", 256, DirtPixel);
            SaveTexture("T_Rock", 256, RockPixel);
            SaveTexture("T_Wood", 256, WoodPixel);

            GetOrCreateTexturedMat("M_Grass", "T_Grass.png", Color.white, 18f, 18f);
            GetOrCreateTexturedMat("M_Path", "T_Dirt.png", Color.white, 1f, 8f);
            GetOrCreateTexturedMat("M_Dirt", "T_Dirt.png", Color.white, 4f, 4f);
            GetOrCreateTexturedMat("M_Rock", "T_Rock.png", Color.white, 6f, 2f);
            GetOrCreateTexturedMat("M_Wood", "T_Wood.png", Color.white, 2f, 2f);
            GetOrCreateFlatMat("M_Leaf", new Color(0.16f, 0.42f, 0.20f));
            GetOrCreateFlatMat("M_LeafTop", new Color(0.27f, 0.56f, 0.28f));
            GetOrCreateFlatMat("M_Trunk", new Color(0.40f, 0.28f, 0.16f));
            GetOrCreateFlatMat("M_Cloud", Color.white);
            GetOrCreateFlatMat("M_FlowerA", new Color(1f, 0.45f, 0.65f));
            GetOrCreateFlatMat("M_FlowerB", new Color(1f, 0.9f, 0.3f));
            GetOrCreateFlatMat("M_FlowerC", Color.white);
            GetOrCreateWaterMat();
            GetOrCreateSkyMat();
        }

        public static Material LoadMat(string name)
        {
            return AssetDatabase.LoadAssetAtPath<Material>($"{MatDir}/{name}.mat");
        }

        // ---------- textures ----------

        delegate Color PixelFunc(int x, int y);

        static void SaveTexture(string name, int size, PixelFunc fn)
        {
            string path = $"{TexDir}/{name}.png";
            if (AssetDatabase.LoadAssetAtPath<Texture2D>(path) != null) return;

            var tex = new Texture2D(size, size, TextureFormat.RGBA32, false);
            tex.wrapMode = TextureWrapMode.Repeat;
            for (int y = 0; y < size; y++)
                for (int x = 0; x < size; x++)
                    tex.SetPixel(x, y, fn(x, y));
            tex.Apply();
            File.WriteAllBytes(Application.dataPath + $"/_AdventureGame/Art/Textures/{name}.png", tex.EncodeToPNG());
            Object.DestroyImmediate(tex);
            AssetDatabase.ImportAsset(path);
            var importer = AssetImporter.GetAtPath(path) as TextureImporter;
            if (importer != null)
            {
                importer.wrapMode = TextureWrapMode.Repeat;
                importer.filterMode = FilterMode.Bilinear;
                importer.SaveAndReimport();
            }
        }

        static Color GrassPixel(int x, int y)
        {
            float n = Fbm(x * 0.06f, y * 0.06f, 11);
            Color c = new Color(0.30f, 0.52f, 0.24f) * (0.82f + 0.36f * n);
            float blade = Hash01(x, y, 77);
            if (blade > 0.93f) c *= 0.68f;
            else if (blade < 0.05f) c += new Color(0.08f, 0.10f, 0.02f);
            c.a = 1f;
            return c;
        }

        static Color DirtPixel(int x, int y)
        {
            float n = Fbm(x * 0.09f, y * 0.09f, 23);
            Color c = new Color(0.48f, 0.36f, 0.23f) * (0.85f + 0.30f * n);
            float s = Hash01(x, y, 31);
            if (s > 0.94f) c = new Color(0.62f, 0.55f, 0.45f);
            else if (s < 0.06f) c *= 0.70f;
            c.a = 1f;
            return c;
        }

        static Color RockPixel(int x, int y)
        {
            float strata = Mathf.Sin((x * 0.3f + y) * 0.12f) * 0.5f + 0.5f;
            float n = Fbm(x * 0.05f, y * 0.05f, 47);
            Color c = new Color(0.45f, 0.45f, 0.47f) * (0.75f + 0.25f * n + 0.15f * strata);
            if (Hash01(x / 3, y / 3, 53) > 0.965f) c *= 0.55f;
            c.a = 1f;
            return c;
        }

        static Color WoodPixel(int x, int y)
        {
            int plank = y / 32;
            float tint = Hash01(plank, 7, 61);
            float grain = Fbm(x * 0.03f, y * 0.25f, 67);
            Color c = new Color(0.55f + tint * 0.10f, 0.38f + tint * 0.07f, 0.22f + tint * 0.05f)
                      * (0.85f + 0.30f * grain);
            if (y % 32 == 0) c *= 0.45f;
            if (Hash01(x, y, 71) > 0.97f) c *= 0.80f;
            c.a = 1f;
            return c;
        }

        // ---------- materials ----------

        static Material GetOrCreateTexturedMat(string matName, string texFile, Color tint, float tileX, float tileY)
        {
            string path = $"{MatDir}/{matName}.mat";
            var existing = AssetDatabase.LoadAssetAtPath<Material>(path);
            if (existing != null) return existing;
            var shader = Shader.Find("Universal Render Pipeline/Lit") ?? Shader.Find("Standard");
            var mat = new Material(shader);
            var tex = AssetDatabase.LoadAssetAtPath<Texture2D>($"{TexDir}/{texFile}");
            if (tex != null)
            {
                if (mat.HasProperty("_BaseMap")) mat.SetTexture("_BaseMap", tex);
                else if (mat.HasProperty("_MainTex")) mat.SetTexture("_MainTex", tex);
                mat.SetTextureScale("_MainTex", new Vector2(tileX, tileY));
                if (mat.HasProperty("_BaseMap"))
                {
                    // URP stores tiling on _BaseMap_ST; mirror it.
                    mat.SetTextureScale("_BaseMap", new Vector2(tileX, tileY));
                }
            }
            if (mat.HasProperty("_BaseColor")) mat.SetColor("_BaseColor", tint);
            else if (mat.HasProperty("_Color")) mat.color = tint;
            AssetDatabase.CreateAsset(mat, path);
            return mat;
        }

        static Material GetOrCreateFlatMat(string matName, Color c)
        {
            string path = $"{MatDir}/{matName}.mat";
            var existing = AssetDatabase.LoadAssetAtPath<Material>(path);
            if (existing != null) return existing;
            var shader = Shader.Find("Universal Render Pipeline/Lit") ?? Shader.Find("Standard");
            var mat = new Material(shader);
            if (mat.HasProperty("_BaseColor")) mat.SetColor("_BaseColor", c);
            else if (mat.HasProperty("_Color")) mat.color = c;
            AssetDatabase.CreateAsset(mat, path);
            return mat;
        }

        static Material GetOrCreateWaterMat()
        {
            const string matName = "M_Water";
            string path = $"{MatDir}/{matName}.mat";
            var existing = AssetDatabase.LoadAssetAtPath<Material>(path);
            if (existing != null) return existing;
            var shader = Shader.Find("Universal Render Pipeline/Lit") ?? Shader.Find("Standard");
            var mat = new Material(shader);
            var c = new Color(0.20f, 0.60f, 0.85f, 0.75f);
            if (mat.HasProperty("_BaseColor")) mat.SetColor("_BaseColor", c);
            else if (mat.HasProperty("_Color")) mat.color = c;
            if (mat.HasProperty("_Surface"))
            {
                mat.SetFloat("_Surface", 1f); // transparent
                mat.SetFloat("_Blend", 0f);   // alpha blend
                mat.SetOverrideTag("RenderType", "Transparent");
                mat.renderQueue = 3000;
            }
            AssetDatabase.CreateAsset(mat, path);
            return mat;
        }

        static Material GetOrCreateSkyMat()
        {
            const string matName = "Sky_Meadow";
            string path = $"{MatDir}/{matName}.mat";
            var existing = AssetDatabase.LoadAssetAtPath<Material>(path);
            if (existing != null) return existing;
            var shader = Shader.Find("Skybox/Procedural");
            Material mat = shader != null ? new Material(shader) : new Material(Shader.Find("Skybox/Cubemap"));
            if (mat.HasProperty("_SkyTint")) mat.SetColor("_SkyTint", new Color(0.35f, 0.55f, 0.85f));
            if (mat.HasProperty("_GroundColor")) mat.SetColor("_GroundColor", new Color(0.45f, 0.5f, 0.45f));
            if (mat.HasProperty("_Exposure")) mat.SetFloat("_Exposure", 1.1f);
            AssetDatabase.CreateAsset(mat, path);
            return mat;
        }

        // ---------- tiny value-noise ----------

        static float Hash01(int x, int y, int s)
        {
            int h = x * 374761393 + y * 668265263 + s * 1442695041;
            h = (h ^ (h >> 13)) * 1274126177;
            h ^= h >> 16;
            return (h & 0x7fffffff) / (float)0x7fffffff;
        }

        static float ValueNoise(float x, float y, int s)
        {
            int xi = (int)Mathf.Floor(x), yi = (int)Mathf.Floor(y);
            float xf = x - xi, yf = y - yi;
            float u = xf * xf * (3f - 2f * xf), v = yf * yf * (3f - 2f * yf);
            float a = Hash01(xi, yi, s), b = Hash01(xi + 1, yi, s);
            float c = Hash01(xi, yi + 1, s), d = Hash01(xi + 1, yi + 1, s);
            return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
        }

        static float Fbm(float x, float y, int s)
        {
            return ValueNoise(x, y, s) * 0.6f
                 + ValueNoise(x * 2.1f, y * 2.1f, s + 7) * 0.28f
                 + ValueNoise(x * 4.3f, y * 4.3f, s + 13) * 0.12f;
        }
    }
}
