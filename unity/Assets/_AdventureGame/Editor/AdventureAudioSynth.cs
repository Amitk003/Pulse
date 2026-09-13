using UnityEngine;
using UnityEditor;
using System.IO;

namespace AdventureGame
{
    /// <summary>
    /// Synthesizes real sound-effect WAV files (no downloads needed) into
    /// Assets/_AdventureGame/Audio/: punch whoosh, punch impact, finisher
    /// boom, guard tink, pickup chime, boss slam, landing thud.
    /// Menu: Adventure &gt; Generate Punch Sounds (also runs in Build).
    /// Re-running is safe (skips existing files).
    /// </summary>
    public static class AdventureAudioSynth
    {
        const int Rate = 22050;

        [MenuItem("Adventure/Generate Punch Sounds")]
        public static void GenerateFromMenu()
        {
            EnsureAudio();
            AssetDatabase.Refresh();
            Debug.Log("[Adventure] Punch sounds ready in Assets/_AdventureGame/Audio/");
        }

        public static void EnsureAudio()
        {
            Directory.CreateDirectory(Application.dataPath + "/_AdventureGame/Audio");
            SaveIfMissing("Whoosh.wav", MakeWhoosh());
            SaveIfMissing("PunchHit.wav", MakeImpact(110f, 55f, 0.20f, 0.9f, 11));
            SaveIfMissing("Finisher.wav", MakeBoom());
            SaveIfMissing("BlockTink.wav", MakeTink());
            SaveIfMissing("Pickup.wav", MakeChime());
            SaveIfMissing("BossSlam.wav", MakeSlam());
            SaveIfMissing("LandThud.wav", MakeImpact(130f, 70f, 0.14f, 0.6f, 77));
        }

        public static AudioClip Load(string fileName)
        {
            return AssetDatabase.LoadAssetAtPath<AudioClip>($"Assets/_AdventureGame/Audio/{fileName}");
        }

        // ---------- recipes ----------

        static float[] MakeWhoosh()
        {
            int n = (int)(Rate * 0.16f);
            var d = new float[n];
            var rand = new System.Random(5);
            float phase = 0f;
            for (int i = 0; i < n; i++)
            {
                float t = (float)i / n;
                phase += 2f * Mathf.PI * Mathf.Lerp(900f, 250f, t) / Rate;
                float env = Mathf.Sin(t * Mathf.PI);
                d[i] = (Mathf.Sin(phase) * 0.5f + ((float)rand.NextDouble() * 2f - 1f) * 0.5f) * env * 0.7f;
            }
            return d;
        }

        static float[] MakeImpact(float f0, float f1, float seconds, float volume, int seed)
        {
            int n = (int)(Rate * seconds);
            var d = new float[n];
            var rand = new System.Random(seed);
            float phase = 0f;
            for (int i = 0; i < n; i++)
            {
                float t = (float)i / n;
                phase += 2f * Mathf.PI * Mathf.Lerp(f0, f1, t) / Rate;
                float env = (1f - t) * (1f - t);
                float snap = t < 0.15f ? ((float)rand.NextDouble() * 2f - 1f) * (1f - t / 0.15f) : 0f;
                d[i] = (Mathf.Sin(phase) * 0.8f + snap * 0.5f) * env * volume;
            }
            return d;
        }

        static float[] MakeBoom()
        {
            int n = (int)(Rate * 0.45f);
            var d = new float[n];
            var rand = new System.Random(21);
            float phase = 0f, lastNoise = 0f;
            for (int i = 0; i < n; i++)
            {
                float t = (float)i / n;
                phase += 2f * Mathf.PI * Mathf.Lerp(70f, 38f, t) / Rate;
                float env = (1f - t) * (1f - t);
                float noise = ((float)rand.NextDouble() * 2f - 1f);
                lastNoise = lastNoise * 0.85f + noise * 0.15f; // dull rumble
                float v = Mathf.Sin(phase) * 0.9f + lastNoise * 0.8f;
                d[i] = Mathf.Clamp(v, -1f, 1f) * env * 0.9f;
            }
            return d;
        }

        static float[] MakeTink()
        {
            int n = (int)(Rate * 0.3f);
            var d = new float[n];
            float[] freqs = { 2093f, 2794f, 3520f };
            for (int i = 0; i < n; i++)
            {
                float t = (float)i / n;
                float env = Mathf.Exp(-t * 9f);
                float v = 0f;
                foreach (float f in freqs) v += Mathf.Sin(2f * Mathf.PI * f * i / Rate);
                d[i] = v / freqs.Length * env * 0.6f;
            }
            return d;
        }

        static float[] MakeChime()
        {
            int n = (int)(Rate * 0.35f);
            var d = new float[n];
            for (int i = 0; i < n; i++)
            {
                float t = (float)i / n;
                float v = 0f;
                if (t < 0.45f) v += Mathf.Sin(2f * Mathf.PI * 880f * i / Rate) * (1f - t / 0.45f);
                if (t > 0.3f)
                {
                    float t2 = (t - 0.3f) / 0.7f;
                    v += Mathf.Sin(2f * Mathf.PI * 1318f * i / Rate) * (1f - t2);
                }
                d[i] = v * 0.5f;
            }
            return d;
        }

        static float[] MakeSlam()
        {
            int n = (int)(Rate * 0.6f);
            var d = new float[n];
            var rand = new System.Random(99);
            float phase = 0f, lastNoise = 0f;
            for (int i = 0; i < n; i++)
            {
                float t = (float)i / n;
                phase += 2f * Mathf.PI * Mathf.Lerp(55f, 30f, t) / Rate;
                float env = (1f - t) * (1f - t);
                float noise = ((float)rand.NextDouble() * 2f - 1f);
                lastNoise = lastNoise * 0.92f + noise * 0.08f;
                float v = Mathf.Sin(phase) + lastNoise * 1.2f;
                d[i] = Mathf.Clamp(v, -1f, 1f) * env * 0.95f;
            }
            return d;
        }

        // ---------- wav file ----------

        static void SaveIfMissing(string fileName, float[] data)
        {
            string assetPath = $"Assets/_AdventureGame/Audio/{fileName}";
            if (AssetDatabase.LoadAssetAtPath<AudioClip>(assetPath) != null) return;
            float peak = 0.001f;
            foreach (float v in data) peak = Mathf.Max(peak, Mathf.Abs(v));
            string full = Application.dataPath + "/_AdventureGame/Audio/" + fileName;
            using (var fs = new FileStream(full, FileMode.Create))
            using (var w = new BinaryWriter(fs))
            {
                int dataBytes = data.Length * 2;
                w.Write(new char[] { 'R', 'I', 'F', 'F' });
                w.Write(36 + dataBytes);
                w.Write(new char[] { 'W', 'A', 'V', 'E' });
                w.Write(new char[] { 'f', 'm', 't', ' ' });
                w.Write(16);
                w.Write((short)1);
                w.Write((short)1);
                w.Write(Rate);
                w.Write(Rate * 2);
                w.Write((short)2);
                w.Write((short)16);
                w.Write(new char[] { 'd', 'a', 't', 'a' });
                w.Write(dataBytes);
                foreach (float v in data)
                    w.Write((short)Mathf.Clamp(v / peak * 0.85f * 32767f, -32768f, 32767f));
            }
            AssetDatabase.ImportAsset(assetPath);
            Debug.Log("[Adventure] Synthesized sound: " + fileName);
        }
    }
}
