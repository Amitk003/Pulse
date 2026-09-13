using UnityEngine;
using UnityEditor;
using UnityEditor.Animations;
using UnityEditor.SceneManagement;

namespace AdventureGame
{
    /// <summary>
    /// Wires the uploaded punch FBX files into the Statter animator:
    /// Humanoid rigs for retargeting + Punch1 (jab) / Punch2 (mirrored cross) /
    /// PunchFinisher (combo clip) states with trigger transitions.
    /// Runs automatically in Build; menu: Adventure &gt; Wire Punch Animations.
    /// Re-running is safe (old punch states are rebuilt).
    /// </summary>
    public static class AdventurePunchSetup
    {
        const string JabPath = "Assets/_AdventureGame/Animation/Punching.fbx";
        const string ComboPath = "Assets/_AdventureGame/Animation/Punch Combo.fbx";
        const string ControllerPath = "Assets/Statter Asset Pack/Animation/Player.controller";

        [MenuItem("Adventure/Wire Punch Animations")]
        public static void WireFromMenu()
        {
            if (EditorApplication.isPlaying)
            {
                EditorUtility.DisplayDialog("Adventure", "Exit Play mode first, then wire again.", "OK");
                return;
            }
            if (WirePunches())
            {
                EditorSceneManager.MarkSceneDirty(EditorSceneManager.GetActiveScene());
                Debug.Log("[Adventure] Punch animations wired. Press Play and click to punch.");
            }
        }

        public static bool WirePunches()
        {
            var jabClip = LoadFirstClip(EnsureHumanoid(JabPath));
            var comboClip = LoadFirstClip(EnsureHumanoid(ComboPath));
            if (jabClip == null || comboClip == null)
            {
                Debug.LogError("[Adventure] Punch clips not found. Put Punching.fbx and Punch Combo.fbx in Assets/_AdventureGame/Animation/ and reimport (Ctrl+R).");
                return false;
            }
            Debug.Log($"[Adventure] Punch clips: jab='{jabClip.name}' ({jabClip.length:F2}s), finisher='{comboClip.name}' ({comboClip.length:F2}s)");

            var controller = AssetDatabase.LoadAssetAtPath<AnimatorController>(ControllerPath);
            if (controller == null)
            {
                Debug.LogError("[Adventure] Missing Statter animator at " + ControllerPath);
                return false;
            }

            AddTrigger(controller, "Punch1");
            AddTrigger(controller, "Punch2");
            AddTrigger(controller, "PunchFinisher");

            var sm = controller.layers[0].stateMachine;
            RemoveState(sm, "Punch1");
            RemoveState(sm, "Punch2");
            RemoveState(sm, "PunchFinisher");

            var movement = FindState(sm, "Movement");
            if (movement == null)
            {
                Debug.LogError("[Adventure] 'Movement' state missing in Statter animator.");
                return false;
            }

            var p1 = sm.AddState("Punch1");
            p1.motion = jabClip;
            p1.speed = 1.35f;
            var p2 = sm.AddState("Punch2");
            p2.motion = jabClip;
            p2.mirror = true; // same clip, other hand
            p2.speed = 1.35f;
            var p3 = sm.AddState("PunchFinisher");
            p3.motion = comboClip;
            p3.speed = 1.1f;

            // Movement -> punches on trigger (soft blends, no snapping).
            AddTriggerTransition(movement, p1, "Punch1", 0.15f);
            AddTriggerTransition(movement, p2, "Punch2", 0.15f);
            AddTriggerTransition(movement, p3, "PunchFinisher", 0.18f);
            // Combo chaining without waiting for exit.
            AddTriggerTransition(p1, p2, "Punch2", 0.12f);
            AddTriggerTransition(p2, p3, "PunchFinisher", 0.12f);
            AddTriggerTransition(p1, p3, "PunchFinisher", 0.12f);
            // Settle back to locomotion.
            AddExitTransition(p1, movement);
            AddExitTransition(p2, movement);
            AddExitTransition(p3, movement);

            EditorUtility.SetDirty(controller);
            AssetDatabase.SaveAssets();
            Debug.Log("[Adventure] Animator rebuilt with Punch1/Punch2/PunchFinisher states.");
            return true;
        }

        public static string EnsureHumanoid(string path)
        {
            var importer = AssetImporter.GetAtPath(path) as ModelImporter;
            if (importer == null)
            {
                Debug.LogError("[Adventure] Not found: " + path);
                return null;
            }
            if (importer.animationType != ModelImporterAnimationType.Human)
            {
                importer.animationType = ModelImporterAnimationType.Human;
                importer.avatarSetup = ModelImporterAvatarSetup.CreateFromThisModel;
                importer.SaveAndReimport();
                Debug.Log("[Adventure] Rig set to Humanoid: " + path);
            }
            return path;
        }

        static AnimationClip LoadFirstClip(string path)
        {
            if (string.IsNullOrEmpty(path)) return null;
            AnimationClip best = null;
            foreach (var o in AssetDatabase.LoadAllAssetsAtPath(path))
            {
                var clip = o as AnimationClip;
                if (clip == null || clip.name.Contains("__preview")) continue;
                Debug.Log($"[Adventure] Found clip '{clip.name}' ({clip.length:F2}s) in {path}");
                if (best == null) best = clip;
            }
            return best;
        }

        static void AddTrigger(AnimatorController controller, string name)
        {
            foreach (var p in controller.parameters)
                if (p.name == name) return;
            controller.AddParameter(name, AnimatorControllerParameterType.Trigger);
        }

        static AnimatorState FindState(AnimatorStateMachine sm, string name)
        {
            foreach (var c in sm.states)
                if (c.state != null && c.state.name == name) return c.state;
            return null;
        }

        static void RemoveState(AnimatorStateMachine sm, string name)
        {
            var s = FindState(sm, name);
            if (s != null) sm.RemoveState(s);
        }

        static void AddTriggerTransition(AnimatorState from, AnimatorState to, string trigger, float duration)
        {
            var t = from.AddTransition(to);
            t.hasExitTime = false;
            t.duration = duration;
            t.AddCondition(AnimatorConditionMode.If, 0f, trigger);
        }

        static void AddExitTransition(AnimatorState from, AnimatorState to)
        {
            var t = from.AddTransition(to);
            t.hasExitTime = true;
            t.exitTime = 0.8f;
            t.duration = 0.3f;
        }
    }
}
