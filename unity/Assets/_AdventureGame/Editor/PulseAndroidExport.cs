#if UNITY_EDITOR
using System.IO;
using System.Linq;
using UnityEditor;
using UnityEngine;

namespace AdventureGame.Editor
{
    public static class PulseAndroidExport
    {
        [MenuItem("Pulse/Export Android Library")]
        public static void ExportAndroidLibrary()
        {
            var projectRoot = Directory.GetParent(Application.dataPath).FullName;
            var exportRoot = Path.Combine(projectRoot, "PulseUnityExport");
            var scenes = EditorBuildSettings.scenes
                .Where(scene => scene.enabled)
                .Select(scene => scene.path)
                .ToArray();

            if (scenes.Length == 0)
                throw new System.InvalidOperationException("No enabled Unity scenes are configured.");

            EditorUserBuildSettings.exportAsGoogleAndroidProject = true;
            var options = new BuildPlayerOptions
            {
                scenes = scenes,
                locationPathName = exportRoot,
                target = BuildTarget.Android,
                options = BuildOptions.AcceptExternalModificationsToPlayer
            };

            var report = BuildPipeline.BuildPlayer(options);
            var resultStr = report.summary.result.ToString();
            if (resultStr != "Succeeded")
                throw new System.InvalidOperationException($"Unity Android export failed: {resultStr}");

            Debug.Log($"Unity Android library exported to {exportRoot}");
        }
    }
}
#endif
