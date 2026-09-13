using UnityEngine;
//This script must be use directly in gameobject with animator in oder to handle root motion
public class PlayerController : MonoBehaviour
{
    float horizontal;
    float vertical;
    float Y;
    Animator animator;
    Vector3 Dir;
    Vector3 Velocity;
    [HideInInspector] public CharacterController characterController;
    [Header(" \n \n \n Make Sure this script is directly attached to the \n \n Gameobject with Animator and Character Controller\n \n ")]
    [SerializeField] Transform Camera;

    [Header("Move")]

    [Tooltip("Walking Animation Blend Speed")]
    [SerializeField] float Move_Smooth = 6;
    [Tooltip("Player Rotation Speed")]
    [SerializeField] float Rotate_Smooth = 4;
    [Tooltip("Player Rotation Speed While Aiming")]
    [SerializeField] float Aim_Rotate_Smooth = 3;
    [Header("Jump")]
    [Tooltip("How High Should Player Go")]
    [SerializeField] float JumpHeight = 1.5f;
    [Tooltip("How Fast Should Player Come Down")]
    [SerializeField] float Gravity = 5;
    [Tooltip("Damper For Player's Forward Movement During Jump While Walking")]
    [SerializeField] float WalkingJumpSpeed = 0.7f;
    [Tooltip("Damper For Player's Forward Movement During Jump While Runing")]
    [SerializeField] float RuningJumpSpeed = 0.5f;
    
    [HideInInspector] public bool Allow_Control;//us this to allow or stop player from taking input
    bool IsJumping;

    void Start()
    {
        animator = GetComponent<Animator>();
        characterController = GetComponent<CharacterController>();
        Allow_Control = true;
    }

    void Update()
    {
        if (Allow_Control)//Allowing User Input
        {
            horizontal = Input.GetAxis("Horizontal");
            vertical = Input.GetAxis("Vertical");
            bool is_sprinting = Input.GetKey(KeyCode.LeftShift);

            if (horizontal != 0 || vertical != 0)
            {
                Rotate(horizontal, vertical);
            }

            Move(is_sprinting, horizontal, vertical, Move_Smooth);

            if (Input.GetKeyDown(KeyCode.Space) && !IsJumping)
            {
                animator.SetTrigger("Jump");
                Jump();
            }
            Debug.Log(characterController.isGrounded);
        }
    }

    void OnAnimatorMove()//Do Not Temper With
    {
        if (IsJumping)
        {            
            Velocity.y -= Gravity * Time.fixedDeltaTime;
            characterController.Move(Velocity * Time.fixedDeltaTime);
            if (characterController.isGrounded)
            {
                IsJumping = false;
                animator.SetTrigger("Jump End");
                Velocity = Vector3.zero;
            }            
        }
        else
        {
            animator.ApplyBuiltinRootMotion();
        }

    }

    void Move(bool Is_Sprinting, float Horizontal, float Vertical, float Speed)
    {
        Dir = new Vector3(Horizontal, 0, Vertical).normalized;
        float TargetY = Is_Sprinting ? Dir.magnitude : Dir.magnitude * 0.5f;

        Y = Mathf.Lerp(Y, TargetY, Time.deltaTime * Speed);
        animator.SetFloat("Y", Y);
    }

    void LookAt()
    {
        float Rotation = Mathf.LerpAngle(transform.eulerAngles.y, Camera.transform.eulerAngles.y, Aim_Rotate_Smooth *Time.deltaTime);
        transform.rotation = Quaternion.Euler(0, Rotation, 0);
    }

    void Rotate(float X, float Y)
    {
        float PlayerInput = Mathf.Atan2(X, Y) * Mathf.Rad2Deg;
        float TargetRotation = Camera.transform.eulerAngles.y + PlayerInput;
        float Rotation = Mathf.LerpAngle(transform.eulerAngles.y, TargetRotation, Time.deltaTime * Rotate_Smooth);
        transform.rotation = Quaternion.Euler(0, Rotation, 0);
    }
    void Jump()
    {
        if (!IsJumping)
        {
            float JumpMultiplayer = Y > 0.5f ? RuningJumpSpeed : WalkingJumpSpeed;
            Velocity = animator.velocity * JumpMultiplayer;
            Velocity.y = Mathf.Sqrt(2 * Gravity * JumpHeight);
            IsJumping = true;
        }
    }
}
