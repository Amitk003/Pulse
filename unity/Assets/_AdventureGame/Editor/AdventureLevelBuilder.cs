using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.InputSystem;
using UnityEngine.InputSystem.UI;
using UnityEngine.UI;
using UnityEditor;
using UnityEditor.SceneManagement;

namespace AdventureGame
{
    /// <summary>
    /// One-click adventure setup. Open SampleScene (or any scene), then run:
    /// Adventure &gt; Build Prototype Level.
    /// Wires the Statter humanoid + InputSystem_Actions + UGUI HUD with no
    /// Cinemachine or NavMesh dependencies.
    /// </summary>
    public static class AdventureLevelBuilder
    {
        const string StatterPrefabPath = "Assets/Statter Asset Pack/Models/Player_Model Prefab.prefab";
        const string ActionsPath = "Assets/InputSystem_Actions.inputactions";

        [MenuItem("Adventure/Build Prototype Level")]
        public static void Build()
        {
            if (EditorApplication.isPlaying)
            {
                EditorUtility.DisplayDialog("Adventure", "Exit Play mode first, then run Build again.", "OK");
                return;
            }
            var scene = EditorSceneManager.GetActiveScene();
            if (!scene.IsValid())
            {
                Debug.LogError("[Adventure] No active scene. Open SampleScene first.");
                return;
            }

            var actions = AssetDatabase.LoadAssetAtPath<InputActionAsset>(ActionsPath);
            if (actions == null)
            {
                Debug.LogError($"[Adventure] Missing Input actions at {ActionsPath}");
                return;
            }
            var statter = AssetDatabase.LoadAssetAtPath<GameObject>(StatterPrefabPath);
            if (statter == null)
            {
                Debug.LogError($"[Adventure] Missing Statter prefab at {StatterPrefabPath}");
                return;
            }

            EnsureEventSystem(actions);
            var cam = EnsureCamera();
            EnsureLight();

            var root = GameObject.Find("Adventure");
            if (root == null)
            {
                root = new GameObject("Adventure");
                Undo.RegisterCreatedObjectUndo(root, "Adventure root");
            }

            BuildEnvironment(root);
            AdventureAudioSynth.EnsureAudio();
            AdventurePunchSetup.WirePunches();
            var player = SpawnPlayer(root, statter, actions, cam);
            SpawnBoss(root);
            RemoveRedCylinders();
            SpawnCollectibles(root);
            // GoalGate (cyan portal) removed by user request — win by slaying the boss.
            // SpawnGoal(root, new Vector3(12f, 0f, 12f));
            var staleGate = GameObject.Find("GoalGate");
            if (staleGate != null) Undo.DestroyObjectImmediate(staleGate);
            EnsureUIAndManager(root, player);
            AdventureWorldBuilder.ApplyMeadowWorld();
            StaticizeWorld(root);

            EditorSceneManager.MarkSceneDirty(scene);
            // Save automatically: an unsaved scene silently discards the whole
            // build on exit, which looks exactly like "nothing happened".
            bool saved = EditorSceneManager.SaveScene(scene);
            Debug.Log("[Adventure] Prototype level built. Scene saved: " + saved + ". Press Play. WASD move, Mouse look, Space jump, Shift sprint, LMB attack, E interact, R restart.");
        }

        [MenuItem("Adventure/Clear Prototype Level")]
        public static void Clear()
        {
            if (EditorApplication.isPlaying)
            {
                EditorUtility.DisplayDialog("Adventure", "Exit Play mode first, then run Clear again.", "OK");
                return;
            }
            foreach (var name in new[] { "Adventure", "GameManager", "AdventureUI" })
            {
                var go = GameObject.Find(name);
                if (go != null) Undo.DestroyObjectImmediate(go);
            }
            EditorSceneManager.MarkSceneDirty(EditorSceneManager.GetActiveScene());
        }

        // ---------- setup helpers ----------

        static void EnsureEventSystem(InputActionAsset actions)
        {
            var es = Object.FindAnyObjectByType<EventSystem>(FindObjectsInactive.Exclude);
            if (es == null)
            {
                var go = new GameObject("EventSystem", typeof(EventSystem), typeof(InputSystemUIInputModule));
                Undo.RegisterCreatedObjectUndo(go, "EventSystem");
                es = go.GetComponent<EventSystem>();
            }
            var old = es.GetComponent<StandaloneInputModule>();
            if (old != null) Object.DestroyImmediate(old);
            var uiModule = es.GetComponent<InputSystemUIInputModule>();
            if (uiModule == null) uiModule = es.gameObject.AddComponent<InputSystemUIInputModule>();
            uiModule.actionsAsset = actions;
        }

        static GameObject EnsureCamera()
        {
            var cam = Camera.main != null ? Camera.main.gameObject : GameObject.Find("Main Camera");
            if (cam == null)
            {
                cam = new GameObject("Main Camera", typeof(Camera), typeof(AudioListener));
                cam.tag = "MainCamera";
                Undo.RegisterCreatedObjectUndo(cam, "Main Camera");
            }
            if (cam.GetComponent<Camera>() == null) cam.AddComponent<Camera>();
            if (cam.GetComponent<AudioListener>() == null) cam.AddComponent<AudioListener>();
            var tp = cam.GetComponent<AdventureThirdPersonCamera>();
            if (tp == null) tp = Undo.AddComponent<AdventureThirdPersonCamera>(cam);
            // Disable the old Statter camera rig if it somehow got added to the main camera.
            var oldCam = cam.GetComponent("CameraController") as MonoBehaviour;
            if (oldCam != null) oldCam.enabled = false;
            return cam;
        }

        static void EnsureLight()
        {
            var light = Object.FindAnyObjectByType<Light>(FindObjectsInactive.Exclude);
            if (light != null) return;
            var go = new GameObject("Directional Light", typeof(Light));
            go.transform.rotation = Quaternion.Euler(50f, -30f, 0f);
            var l = go.GetComponent<Light>();
            l.type = LightType.Directional;
            l.intensity = 2f;
            Undo.RegisterCreatedObjectUndo(go, "Directional Light");
        }

        static Material Lit(Color c, float emission = 0f)
        {
            var shader = Shader.Find("Universal Render Pipeline/Lit") ?? Shader.Find("Standard");
            var m = new Material(shader);
            if (m.HasProperty("_BaseColor")) m.SetColor("_BaseColor", c);
            if (m.HasProperty("_Color")) m.color = c;
            if (emission > 0f && m.HasProperty("_EmissionColor"))
            {
                m.SetColor("_EmissionColor", c * emission);
                m.EnableKeyword("_EMISSION");
            }
            return m;
        }

        static GameObject SolidBox(GameObject parent, string name, Vector3 pos, Vector3 size, Color color)
        {
            var go = GameObject.CreatePrimitive(PrimitiveType.Cube);
            go.name = name;
            go.transform.SetParent(parent.transform, false);
            go.transform.position = pos;
            go.transform.localScale = size;
            go.GetComponent<Renderer>().sharedMaterial = Lit(color);
            go.isStatic = true; // static batching: free draw calls
            return go;
        }

        static void BuildEnvironment(GameObject root)
        {
            var env = root.transform.Find("Environment");
            if (env != null) return; // already built
            env = new GameObject("Environment").transform;
            env.SetParent(root.transform, false);

            // Ground
            var ground = SolidBox(root, "Ground", new Vector3(0f, -0.5f, 0f), new Vector3(44f, 1f, 44f), new Color(0.25f, 0.35f, 0.3f));
            ground.transform.SetParent(env, false);

            // Floating platforms (parkour flavor from the Demo scene)
            SolidBox(root, "Platform_A", new Vector3(-6f, 1.2f, 6f), new Vector3(4f, 0.4f, 4f), new Color(0.4f, 0.4f, 0.5f)).transform.SetParent(env, false);
            SolidBox(root, "Platform_B", new Vector3(6f, 2.2f, -2f), new Vector3(4f, 0.4f, 4f), new Color(0.4f, 0.4f, 0.5f)).transform.SetParent(env, false);

            // Stairs: 3 steps up to Platform_B side
            for (int i = 0; i < 3; i++)
                SolidBox(root, $"Stair_{i}", new Vector3(2.5f + i * 1.1f, 0.25f + i * 0.5f, -2f), new Vector3(1.1f, 0.5f + i * 0.5f, 2f), new Color(0.5f, 0.45f, 0.35f)).transform.SetParent(env, false);

            // Low boundary walls so the player/enemies stay in
            SolidBox(root, "Wall_N", new Vector3(0f, 1f, -21f), new Vector3(44f, 2f, 1f), new Color(0.3f, 0.3f, 0.35f)).transform.SetParent(env, false);
            SolidBox(root, "Wall_S", new Vector3(0f, 1f, 21f), new Vector3(44f, 2f, 1f), new Color(0.3f, 0.3f, 0.35f)).transform.SetParent(env, false);
            SolidBox(root, "Wall_E", new Vector3(21f, 1f, 0f), new Vector3(1f, 2f, 44f), new Color(0.3f, 0.3f, 0.35f)).transform.SetParent(env, false);
            SolidBox(root, "Wall_W", new Vector3(-21f, 1f, 0f), new Vector3(1f, 2f, 44f), new Color(0.3f, 0.3f, 0.35f)).transform.SetParent(env, false);
        }

        const string SurvivalPath = "Assets/_AdventureGame/Characters/Survival_Character.fbx";
        const string StatterControllerPath = "Assets/Statter Asset Pack/Animation/Player.controller";

        static GameObject SpawnPlayer(GameObject root, GameObject statter, InputActionAsset actions, GameObject cam)
        {
            // Survival character takes over when present; dummy is the fallback.
            var survival = AssetDatabase.LoadAssetAtPath<GameObject>(SurvivalPath);
            if (survival == null && System.IO.File.Exists(Application.dataPath + "/_AdventureGame/Characters/Survival_Character.fbx"))
                Debug.LogWarning("[Adventure] Survival FBX on disk but not imported yet — press Ctrl+R in Unity, then Build again.");
            if (survival != null) return SpawnSurvivalPlayer(root, actions, cam);

            var existing = GameObject.Find("Player (Adventure)");
            if (existing != null) return existing;

            var player = (GameObject)PrefabUtility.InstantiatePrefab(statter);
            player.name = "Player (Adventure)";
            player.tag = "Player";
            player.transform.SetParent(root.transform, false);
            player.transform.position = new Vector3(0f, 0.1f, 0f);
            Undo.RegisterCreatedObjectUndo(player, "Adventure player");

            // Turn off the legacy root-motion script; our controller drives the same Animator.
            var legacy = player.GetComponent("PlayerController") as MonoBehaviour;
            if (legacy != null) legacy.enabled = false;

            var input = player.GetComponent<PlayerInput>();
            if (input == null) input = player.AddComponent<PlayerInput>();
            input.actions = actions;
            input.defaultActionMap = "Player";
            input.notificationBehavior = PlayerNotifications.InvokeUnityEvents;

            var controller = player.GetComponent<AdventurePlayerController>();
            if (controller == null) controller = player.AddComponent<AdventurePlayerController>();
            controller.playerInput = input;
            controller.cameraTransform = cam.transform;
            controller.whooshSound = AdventureAudioSynth.Load("Whoosh.wav");
            controller.hitSound = AdventureAudioSynth.Load("punch.mp3");
            controller.finisherSound = AdventureAudioSynth.Load("Finisher.wav");
            controller.blockSound = AdventureAudioSynth.Load("BlockTink.wav");
            controller.landSound = AdventureAudioSynth.Load("LandThud.wav");

            var tp = cam.GetComponent<AdventureThirdPersonCamera>();
            tp.target = player.transform;
            tp.playerInput = input;

            // Guarantee a CharacterController (prefab already has one).
            if (player.GetComponent<CharacterController>() == null)
                player.AddComponent<CharacterController>();

            return player;
        }

        static GameObject SpawnSurvivalPlayer(GameObject root, InputActionAsset actions, GameObject cam)
        {
            var existing = GameObject.Find("Player (Adventure)");
            if (existing != null && existing.transform.Find("SurvivalModel") != null)
            {
                // Already swapped: repaint in place (fixes white) + rewire camera.
                ApplySurvivalSkin(existing.transform.Find("SurvivalModel").gameObject);
                var tpOld = cam.GetComponent<AdventureThirdPersonCamera>();
                tpOld.target = existing.transform;
                var inputOld = existing.GetComponent<PlayerInput>();
                if (inputOld != null) tpOld.playerInput = inputOld;
                return existing;
            }
            if (existing != null)
            {
                Object.DestroyImmediate(existing);
                Debug.Log("[Adventure] Replacing dummy with the Survival character.");
            }

            AdventurePunchSetup.EnsureHumanoid(SurvivalPath);
            EnsureSurvivalMaterialImport();

            var player = new GameObject("Player (Adventure)");
            player.tag = "Player";
            player.transform.SetParent(root.transform, false);
            player.transform.position = new Vector3(0f, 0.1f, 0f);
            Undo.RegisterCreatedObjectUndo(player, "Survival player");

            var model = AssetDatabase.LoadAssetAtPath<GameObject>(SurvivalPath);
            var visual = (GameObject)PrefabUtility.InstantiatePrefab(model);
            visual.name = "SurvivalModel";
            visual.transform.SetParent(player.transform, false);
            visual.transform.localPosition = Vector3.zero;
            visual.transform.localRotation = Quaternion.identity;
            foreach (var c in visual.GetComponentsInChildren<Collider>())
                Object.DestroyImmediate(c);
            ApplySurvivalSkin(visual);

            // Same brain, new body: Statter controller retargets onto his avatar.
            Avatar avatar = null;
            foreach (var o in AssetDatabase.LoadAllAssetsAtPath(SurvivalPath))
                if (o is Avatar a) { avatar = a; break; }
            var anim = player.AddComponent<Animator>();
            var ctrl = AssetDatabase.LoadAssetAtPath<RuntimeAnimatorController>(StatterControllerPath);
            if (ctrl != null) anim.runtimeAnimatorController = ctrl;
            else Debug.LogError("[Adventure] Missing Statter animator: " + StatterControllerPath);
            if (avatar != null) anim.avatar = avatar;
            else Debug.LogError("[Adventure] No avatar in Survival FBX — set Rig to Humanoid and reimport (Ctrl+R).");
            anim.applyRootMotion = false;

            // Fit: ground the feet, size the capsule to the real model.
            var renderers = visual.GetComponentsInChildren<Renderer>();
            float h = 1.8f;
            if (renderers.Length > 0)
            {
                Bounds b = renderers[0].bounds;
                foreach (var r in renderers) b.Encapsulate(r.bounds);
                if (b.size.y > 0.01f)
                {
                    h = b.size.y;
                    Vector3 lp = visual.transform.localPosition;
                    lp.y += 0.02f - b.min.y;
                    visual.transform.localPosition = lp;
                    Debug.Log($"[Adventure] Survival measured height: {h:F2}m.");
                }
            }
            var cc = player.AddComponent<CharacterController>();
            cc.height = Mathf.Max(1.2f, h);
            cc.radius = Mathf.Clamp(h * 0.14f, 0.2f, 0.4f);
            cc.center = new Vector3(0f, cc.height * 0.5f + 0.02f, 0f);

            var input = player.AddComponent<PlayerInput>();
            input.actions = actions;
            input.defaultActionMap = "Player";
            input.notificationBehavior = PlayerNotifications.InvokeUnityEvents;

            var controller = player.AddComponent<AdventurePlayerController>();
            controller.playerInput = input;
            controller.cameraTransform = cam.transform;
            controller.whooshSound = AdventureAudioSynth.Load("Whoosh.wav");
            controller.hitSound = AdventureAudioSynth.Load("punch.mp3");
            controller.finisherSound = AdventureAudioSynth.Load("Finisher.wav");
            controller.blockSound = AdventureAudioSynth.Load("BlockTink.wav");
            controller.landSound = AdventureAudioSynth.Load("LandThud.wav");

            var tp = cam.GetComponent<AdventureThirdPersonCamera>();
            tp.target = player.transform;
            tp.playerInput = input;

            Debug.Log("[Adventure] Survival character is the player now. Run, jump, punch, guard — same setup, new body.");
            return player;
        }

        // Survival FBX ships with material import off (white default).
        // Flip it on, then paint each slot by name (skin, hair, jacket...).
        static void EnsureSurvivalMaterialImport()
        {
            var importer = AssetImporter.GetAtPath(SurvivalPath) as ModelImporter;
            if (importer != null && importer.materialImportMode != ModelImporterMaterialImportMode.ImportViaMaterialDescription)
            {
                importer.materialImportMode = ModelImporterMaterialImportMode.ImportViaMaterialDescription;
                importer.SaveAndReimport();
                Debug.Log("[Adventure] Survival material import enabled.");
            }
        }

        static void ApplySurvivalSkin(GameObject visual)
        {
            int painted = 0, kept = 0;
            foreach (var r in visual.GetComponentsInChildren<Renderer>())
            {
                var slots = r.sharedMaterials;
                for (int i = 0; i < slots.Length; i++)
                {
                    Color? mapped = MapSurvivalSlot(slots[i] != null ? slots[i].name : "");
                    if (mapped.HasValue) { slots[i] = AdventureNatureBuilder.GetVCMaterial(mapped.Value); painted++; }
                    else kept++;
                }
                r.sharedMaterials = slots;
            }
            Debug.Log($"[Adventure] Survival skin painted: {painted} slots, {kept} kept.");
        }

        static Color? MapSurvivalSlot(string slotName)
        {
            string n = (slotName ?? "").ToLowerInvariant();
            if (n.Contains("mouth")) return new Color(0.45f, 0.22f, 0.20f);
            if (n.Contains("head") || n.Contains("body") || n.Contains("arm") || n.Contains("hand") || n.Contains("skin"))
                return new Color(0.80f, 0.60f, 0.46f);
            if (n.Contains("hair") || n.Contains("brow") || n.Contains("leash") || n.Contains("beard"))
                return new Color(0.22f, 0.14f, 0.09f);
            if (n.Contains("jacket") || n.Contains("shirt") || n.Contains("top"))
                return new Color(0.45f, 0.42f, 0.30f);
            if (n.Contains("jean") || n.Contains("pant") || n.Contains("trouser"))
                return new Color(0.24f, 0.34f, 0.55f);
            if (n.Contains("shoe") || n.Contains("glove") || n.Contains("boot"))
                return new Color(0.18f, 0.14f, 0.11f);
            if (n.Contains("backpack") || n.Contains("pack") || n.Contains("bag"))
                return new Color(0.52f, 0.46f, 0.32f);
            return null; // unknown slot: keep imported material
        }

        static void SpawnBoss(GameObject root)
        {
            // The friendly NPC is gone for good, whether or not the boss spawns.
            var oldNpc = GameObject.Find("Y-Bot (Friendly NPC)");
            if (oldNpc != null)
            {
                Object.DestroyImmediate(oldNpc);
                Debug.Log("[Adventure] Friendly NPC removed.");
            }
            if (GameObject.Find("Boss_Monster01") != null)
            {
                // Upgrade pass: components + fit + beacon on the existing boss.
                var existing = GameObject.Find("Boss_Monster01");
                EnsureBossComponents(existing);
                FitBossToModel(existing, root);
                EnsureBossBeacon(root);
                EditorSceneManager.MarkSceneDirty(EditorSceneManager.GetActiveScene());
                return;
            }
            const string prefabPath = "Assets/Stylized3DMonster/Monster01/Prefab/Monster01.prefab";
            var prefab = AssetDatabase.LoadAssetAtPath<GameObject>(prefabPath);
            if (prefab == null)
            {
                Debug.LogError("[Adventure] Boss prefab missing. Import Monster01_Pack.unitypackage first.");
                return;
            }
            var boss = (GameObject)PrefabUtility.InstantiatePrefab(prefab);
            boss.name = "Boss_Monster01";
            boss.transform.SetParent(root.transform, false);
            boss.transform.position = new Vector3(-11f, 0.1f, 11f);
            boss.transform.rotation = Quaternion.Euler(0f, 135f, 0f);
            Undo.RegisterCreatedObjectUndo(boss, "Boss Monster01");

            EnsureBossComponents(boss);
            FitBossToModel(boss, root);
            EnsureBossBeacon(root);

            Debug.Log("[Adventure] Boss spawned at the north-west arena. Kill it to win instantly.");
        }

        static void EnsureBossComponents(GameObject boss)
        {
            const string controllerPath = "Assets/Stylized3DMonster/Monster01/Anim/Monster01_AC.controller";
            var ctrl = AssetDatabase.LoadAssetAtPath<RuntimeAnimatorController>(controllerPath);
            if (ctrl == null) Debug.LogError("[Adventure] Boss animator controller missing at " + controllerPath);

            // Clip paths are relative to the 'root' bone: the Animator MUST sit
            // on its parent, or no animation plays at all.
            Transform target = boss.transform;
            var rootBone = FindDeep(boss.transform, "root");
            if (rootBone != null && rootBone.parent != null) target = rootBone.parent;
            else if (rootBone == null) Debug.LogWarning("[Adventure] 'root' bone not found on boss; anims may not play.");

            // Remove stray Animators so exactly one drives the model.
            foreach (var a in boss.GetComponentsInChildren<Animator>())
                if (a.gameObject != target.gameObject) Object.DestroyImmediate(a);
            var anim = target.GetComponent<Animator>();
            if (anim == null) anim = target.gameObject.AddComponent<Animator>();
            if (ctrl != null) anim.runtimeAnimatorController = ctrl;

            if (boss.GetComponent<CharacterController>() == null) boss.AddComponent<CharacterController>();
            if (boss.GetComponent<BossEnemy>() == null) boss.AddComponent<BossEnemy>();
            var bossScript = boss.GetComponent<BossEnemy>();
            if (bossScript != null) bossScript.slamSound = AdventureAudioSynth.Load("BossSlam.wav");
            AssignBossSkin(boss, 1);
        }

        // The model ships with no materials (white default). Dress it in the
        // pack's own cel-shaded skin. Change variant 1-9 for a different look.
        static void AssignBossSkin(GameObject boss, int variant)
        {
            var mat = AssetDatabase.LoadAssetAtPath<Material>(
                $"Assets/Stylized3DMonster/Monster01/Shader_Texture/Shader/Monster01_0{variant}.mat");
            if (mat == null)
            {
                Debug.LogError("[Adventure] Boss skin missing: Monster01_0" + variant);
                return;
            }
            int count = 0;
            foreach (var r in boss.GetComponentsInChildren<Renderer>())
            {
                var slots = r.sharedMaterials;
                for (int i = 0; i < slots.Length; i++) slots[i] = mat;
                r.sharedMaterials = slots;
                count++;
            }
            Debug.Log($"[Adventure] Boss skinned with Monster01_0{variant} on {count} renderers.");
        }

        static Transform FindDeep(Transform root, string targetName)
        {
            foreach (var t in root.GetComponentsInChildren<Transform>(true))
                if (t.name == targetName) return t;
            return null;
        }

        // Measure the real model, scale to ~2.8m, ground it, fit the capsule.
        static void FitBossToModel(GameObject boss, GameObject root)
        {
            var cc = boss.GetComponent<CharacterController>();
            if (cc == null) cc = boss.AddComponent<CharacterController>();
            var renderers = boss.GetComponentsInChildren<Renderer>();
            Debug.Log($"[Adventure] Boss model has {renderers.Length} renderers.");
            if (renderers.Length == 0)
            {
                Debug.LogError("[Adventure] Boss model has NO meshes — check Monster01_AllAnim.fbx import.");
                cc.height = 2.6f;
                cc.radius = 0.7f;
                cc.center = new Vector3(0f, 1.3f, 0f);
                return;
            }
            Bounds b = renderers[0].bounds;
            foreach (var r in renderers) b.Encapsulate(r.bounds);
            Debug.Log($"[Adventure] Boss measured height: {b.size.y:F2}m.");
            if (b.size.y <= 0.01f) return;
            float s = 2.8f / b.size.y;
            boss.transform.localScale = Vector3.one * s;
            // Re-measure and sit the feet on the ground.
            b = renderers[0].bounds;
            foreach (var r in renderers) b.Encapsulate(r.bounds);
            Vector3 p = boss.transform.position;
            p.y += 0.05f - b.min.y;
            boss.transform.position = p;
            cc.height = b.size.y;
            cc.radius = Mathf.Max(0.5f, b.size.x * 0.3f);
            cc.center = new Vector3(0f, cc.height * 0.5f + 0.05f, 0f);
            EditorSceneManager.MarkSceneDirty(EditorSceneManager.GetActiveScene());
        }

        static void EnsureBossBeacon(GameObject root)
        {
            // Red beacon pillar removed by user request — the boss HP bar
            // and aggro message already announce the arena.
            var stale = GameObject.Find("BossBeacon");
            if (stale != null) Undo.DestroyObjectImmediate(stale);
        }

        // Old red capsule enemies, retired: the boss is the fight now.
        static void RemoveRedCylinders()
        {
            foreach (var n in new[] { "Enemy_1", "Enemy_2" })
            {
                var e = GameObject.Find(n);
                if (e != null)
                {
                    Object.DestroyImmediate(e);
                    Debug.Log("[Adventure] Removed " + n + ".");
                }
            }
        }

        static void SpawnEnemies(GameObject root)
        {
            SpawnEnemy(root, "Enemy_1", new Vector3(-7f, 1f, -5f));
            SpawnEnemy(root, "Enemy_2", new Vector3(7f, 1f, 7f));
        }

        static void SpawnEnemy(GameObject root, string name, Vector3 pos)
        {
            if (GameObject.Find(name) != null) return;
            var go = GameObject.CreatePrimitive(PrimitiveType.Capsule);
            go.name = name;
            go.transform.SetParent(root.transform, false);
            go.transform.position = pos;
            Undo.RegisterCreatedObjectUndo(go, name);
            var primitiveCol = go.GetComponent<CapsuleCollider>();
            if (primitiveCol != null) Object.DestroyImmediate(primitiveCol);
            var cc = go.AddComponent<CharacterController>();
            cc.height = 1.8f; cc.radius = 0.4f; cc.center = new Vector3(0f, 0.9f, 0f);
            go.GetComponent<Renderer>().sharedMaterial = Lit(Color.red);
            var enemy = go.AddComponent<EnemyChaser>();
            enemy.patrolOffsets = new[] { new Vector3(-4f, 0f, 0f), new Vector3(4f, 0f, 0f) };
        }

        static void SpawnCollectibles(GameObject root)
        {
            Vector3[] spots =
            {
                new Vector3(-6f, 1f, 6f),
                new Vector3(-2f, 1f, -6f),
                new Vector3(6f, 3.1f, -2f),   // on Platform_B
                new Vector3(3f, 1f, -2f),     // near stairs
                new Vector3(9f, 1f, 8f),      // near goal
            };
            // Shared metallic gold: color + full metal + smooth + warm glow.
            var goldShader = Shader.Find("Universal Render Pipeline/Lit") ?? Shader.Find("Standard");
            var gold = new Material(goldShader);
            if (gold.HasProperty("_BaseColor")) gold.SetColor("_BaseColor", new Color(1f, 0.76f, 0.28f));
            else if (gold.HasProperty("_Color")) gold.color = new Color(1f, 0.76f, 0.28f);
            if (gold.HasProperty("_Metallic")) gold.SetFloat("_Metallic", 1f);
            if (gold.HasProperty("_Smoothness")) gold.SetFloat("_Smoothness", 0.65f);
            if (gold.HasProperty("_EmissionColor"))
            {
                gold.SetColor("_EmissionColor", new Color(0.55f, 0.35f, 0.08f));
                gold.EnableKeyword("_EMISSION");
            }
            for (int i = 0; i < spots.Length; i++)
            {
                string name = $"Shard_{i + 1}";
                var old = GameObject.Find(name);
                if (old != null)
                {
                    if (old.transform.localScale.y < 0.3f) continue; // already a coin
                    Object.DestroyImmediate(old); // upgrade cube -> coin
                }
                var go = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
                go.name = name;
                go.transform.SetParent(root.transform, false);
                go.transform.position = spots[i];
                go.transform.localScale = new Vector3(0.8f, 0.12f, 0.8f); // coin: wide, thin
                go.transform.rotation = Quaternion.Euler(0f, 45f, 0f);
                var cap = go.GetComponent<CapsuleCollider>();
                if (cap != null) Object.DestroyImmediate(cap);
                go.GetComponent<Renderer>().sharedMaterial = gold;
                go.GetComponent<Renderer>().shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
                Undo.RegisterCreatedObjectUndo(go, name);
                var c = go.AddComponent<Collectible>(); // adds trigger SphereCollider
                c.scoreValue = 1;
                c.healAmount = 10; // sustain for the boss fight
            }
        }

        static void SpawnGoal(GameObject root, Vector3 pos)
        {
            if (GameObject.Find("GoalGate") != null) return;
            var go = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            go.name = "GoalGate";
            go.transform.SetParent(root.transform, false);
            go.transform.position = pos + Vector3.up * 1.5f;
            go.transform.localScale = new Vector3(3f, 3f, 3f);
            var cyl = go.GetComponent<CapsuleCollider>();
            if (cyl != null) Object.DestroyImmediate(cyl);
            var mat = Lit(Color.cyan, 1.2f);
            // Transparent-ish gate
            if (mat.HasProperty("_Surface"))
            {
                mat.SetFloat("_Surface", 1f); // transparent
                mat.SetFloat("_Blend", 0f);
            }
            go.GetComponent<Renderer>().sharedMaterial = mat;
            Undo.RegisterCreatedObjectUndo(go, "GoalGate");
            go.AddComponent<GoalPortal>(); // adds trigger BoxCollider
        }

        // Static batching for everything that never moves (also upgrades old builds).
        // Movers (player, boss, shards, goal, clouds) are intentionally skipped.
        static void StaticizeWorld(GameObject root)
        {
            var env = root.transform.Find("Environment");
            if (env != null)
                foreach (Transform t in env.GetComponentsInChildren<Transform>(true))
                    t.gameObject.isStatic = true;
            var world = root.transform.Find("World");
            if (world != null)
                foreach (Transform t in world.GetComponentsInChildren<Transform>(true))
                {
                    if (t.name == "Cloud" || t.name == "GoalGate" || t.name.StartsWith("Shard_")) continue;
                    t.gameObject.isStatic = true;
                }
        }

        static void EnsureUIAndManager(GameObject root, GameObject player)
        {
            var canvasGo = GameObject.Find("AdventureUI");
            if (canvasGo == null)
            {
                canvasGo = new GameObject("AdventureUI", typeof(Canvas), typeof(CanvasScaler), typeof(GraphicRaycaster), typeof(AdventureUI));
                Undo.RegisterCreatedObjectUndo(canvasGo, "AdventureUI");
                var canvas = canvasGo.GetComponent<Canvas>();
                canvas.renderMode = RenderMode.ScreenSpaceOverlay;
                var scaler = canvasGo.GetComponent<CanvasScaler>();
                scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
                scaler.referenceResolution = new Vector2(1280f, 720f);
            }

            var mgrGo = GameObject.Find("GameManager");
            if (mgrGo == null)
            {
                mgrGo = new GameObject("GameManager", typeof(AdventureGameManager), typeof(StartScreen));
                Undo.RegisterCreatedObjectUndo(mgrGo, "GameManager");
            }
            if (mgrGo.GetComponent<StartScreen>() == null) mgrGo.AddComponent<StartScreen>();
            var mgr = mgrGo.GetComponent<AdventureGameManager>();
            mgr.player = player;
            mgr.pickupSound = AdventureAudioSynth.Load("Pickup.wav");
            if (player != null) mgr.spawnPoint = player.transform.position;
        }
    }
}
