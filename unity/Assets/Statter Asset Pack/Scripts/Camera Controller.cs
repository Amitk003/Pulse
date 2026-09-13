using UnityEngine;

public class CameraController : MonoBehaviour
{
    [Header("Camera Movement")]

    [Tooltip("Camera Offset")]
    [SerializeField] Vector3 Camera_Offset;
    [Tooltip("Camera Follow Speed Use A Higher Value For Continues Flowing And Lower Value For a Damping Effect")]
    [SerializeField] float Follow_Speed; //Higher speed for instant and lower for a dampning effect;

    [Header("Camera Rotation")]

    [Tooltip("The Rotation Speed For Camera")]
    [SerializeField] float Rotation_Speed = 30;
    [Tooltip("Maximum Value Camera Can Rotate in positve X Direction")]
    [SerializeField] float Top_Clamp = 70;
    [Tooltip("Maximum Value Camera Can Rotate in Negative X Direction")]
    [SerializeField] float Bottom_Clamp = -40;

    [Header("Camera Refrences")]

    [Tooltip("Just An Empty Gameobject To Be Use For Camera Rotation And Position NOTE - The Game Object Should Not Have A Parent Attached")]
    [SerializeField] Transform CameraTarget;

    [Tooltip("Camera That You Want TO be Trigger After Pressing Aim")]
    [SerializeField] GameObject AimCam;
    [Tooltip("Player")]
    
    [SerializeField] Transform Player;

    float Pich;
    float Yaw;
    
    void Start()
    {
        AimCam.SetActive(false);
    }
    void Update()
    {

    }

    void LateUpdate()
    {
        CameraFollow();
        CameraRotate();
        Aim();
    }

    void CameraFollow()
    {
        Vector3 FollowVector = Player.position + Camera_Offset;
        CameraTarget.position = Vector3.Lerp(CameraTarget.position, FollowVector, Follow_Speed * Time.deltaTime);
    }

    void CameraRotate()
    {
        float MouseX = GetMouseInput("Mouse X");
        float MouseY = GetMouseInput("Mouse Y");

        Pich = ClampRotaion(Pich, MouseY, Bottom_Clamp, Top_Clamp, true);
        Yaw = ClampRotaion(Yaw, MouseX, float.MinValue, float.MaxValue, false);

        ApplyRotation(Pich, Yaw);
    }

    void ApplyRotation(float Pich, float Yaw)
    {
        CameraTarget.transform.rotation = Quaternion.Euler(Pich, Yaw, CameraTarget.eulerAngles.z);
    }
    float ClampRotaion(float CurrntRotaition, float Input, float Min, float Max, bool IsAxis)
    {
        CurrntRotaition += IsAxis ? -Input : Input;
        return Mathf.Clamp(CurrntRotaition, Min, Max);
    }

    float GetMouseInput(string Axis)
    {
        return Input.GetAxis(Axis) * Rotation_Speed * 10 * Time.deltaTime;
    }

    void Aim()
    {
        if (Input.GetMouseButtonDown(1))
        {
            AimCam.SetActive(true);
        }
        else if (Input.GetMouseButtonUp(1))
        {
            AimCam.SetActive(false);
        }
    }
}
