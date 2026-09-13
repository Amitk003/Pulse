using UnityEngine;

namespace AdventureGame
{
    /// <summary>
    /// Procedural heater-shield mesh (rounded top, tapered bottom point),
    /// so the guard reads as a shield at a glance. Runtime-safe.
    /// </summary>
    public static class GuardShieldMesh
    {
        public static Mesh CreateHeaterShield()
        {
            // Outline in local space: ~0.6 wide, ~0.77 tall, faces +Z.
            Vector2[] outline = {
                new Vector2(-0.28f, 0.375f), new Vector2(-0.15f, 0.39f),
                new Vector2(0f, 0.395f), new Vector2(0.15f, 0.39f),
                new Vector2(0.28f, 0.375f), new Vector2(0.30f, 0.15f),
                new Vector2(0.26f, -0.05f), new Vector2(0.16f, -0.22f),
                new Vector2(0f, -0.375f),
                new Vector2(-0.16f, -0.22f), new Vector2(-0.26f, -0.05f),
                new Vector2(-0.30f, 0.15f),
            };
            const float thick = 0.05f;
            int n = outline.Length;

            var verts = new System.Collections.Generic.List<Vector3>(n * 2 + 2);
            var tris = new System.Collections.Generic.List<int>();
            var uvs = new System.Collections.Generic.List<Vector2>();

            // Front (z+) and back (z-) centers for the triangle fans.
            verts.Add(new Vector3(0f, 0.02f, thick * 0.5f)); uvs.Add(new Vector2(0.5f, 0.5f));
            verts.Add(new Vector3(0f, 0.02f, -thick * 0.5f)); uvs.Add(new Vector2(0.5f, 0.5f));
            for (int i = 0; i < n; i++)
            {
                verts.Add(new Vector3(outline[i].x, outline[i].y, thick * 0.5f));
                uvs.Add(new Vector2(outline[i].x + 0.5f, outline[i].y + 0.5f));
            }
            for (int i = 0; i < n; i++)
            {
                verts.Add(new Vector3(outline[i].x, outline[i].y, -thick * 0.5f));
                uvs.Add(new Vector2(outline[i].x + 0.5f, outline[i].y + 0.5f));
            }
            // Front fan (CCW from +Z) and back fan (reversed).
            for (int i = 0; i < n; i++)
            {
                int a = 2 + i, b = 2 + (i + 1) % n;
                tris.Add(0); tris.Add(a); tris.Add(b);
                int c = 2 + n + i, d = 2 + n + (i + 1) % n;
                tris.Add(1); tris.Add(d); tris.Add(c);
            }
            // Rim quads.
            for (int i = 0; i < n; i++)
            {
                int f0 = 2 + i, f1 = 2 + (i + 1) % n;
                int b0 = 2 + n + i, b1 = 2 + n + (i + 1) % n;
                tris.Add(f0); tris.Add(b0); tris.Add(b1);
                tris.Add(f0); tris.Add(b1); tris.Add(f1);
            }

            var mesh = new Mesh();
            mesh.name = "GuardShield";
            mesh.SetVertices(verts);
            mesh.SetTriangles(tris, 0);
            mesh.SetUVs(0, uvs);
            mesh.RecalculateNormals();
            mesh.RecalculateBounds();
            return mesh;
        }
    }
}
