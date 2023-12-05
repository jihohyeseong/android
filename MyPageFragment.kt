package com.example.hyeseong.myPage

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.hyeseong.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.hyeseong.R
import com.example.hyeseong.databinding.FragmentMypageBinding
import com.example.hyeseong.home.HomeFragment

class MyPageFragment : Fragment(R.layout.fragment_mypage) {


    private lateinit var binding: FragmentMypageBinding
    private lateinit var nameEditText: EditText
    private lateinit var birthdateEditText: EditText

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentMypageBinding = FragmentMypageBinding.bind(view)
        binding = fragmentMypageBinding
        nameEditText = fragmentMypageBinding.nameEditText
        birthdateEditText = fragmentMypageBinding.birthdateEditText

        initSignInOutButton()

        initSignUpButton()

        initEmailEditText()

        initPasswordEditText()
    }

    private fun initPasswordEditText() {
        binding.passwordEditText.addTextChangedListener {
            val enable =
                binding.emailEditText.text.isNotEmpty() && binding.passwordEditText.text.isNotEmpty()
            binding.signUpButton.isEnabled = enable
            binding.signInOutButton.isEnabled = enable
        }
    }

    private fun initEmailEditText() {

        binding.emailEditText.addTextChangedListener {
            val enable =
                binding.emailEditText.text.isNotEmpty() && binding.passwordEditText.text.isNotEmpty()
            binding.signUpButton.isEnabled = enable
            binding.signInOutButton.isEnabled = enable
        }
    }

    private fun initSignUpButton() {
        binding.signUpButton.setOnClickListener {

            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val name = nameEditText.text.toString()
            val birthdate = birthdateEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && birthdate.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {

                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(requireActivity()) { signInTask ->
                                    if (signInTask.isSuccessful) {
                                        Toast.makeText(
                                            context,
                                            "회원가입 및 로그인에 성공했습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        val homeFragment = HomeFragment()
                                        activity?.supportFragmentManager?.beginTransaction()
                                            ?.replace(R.id.fragmentContainer, homeFragment)
                                            ?.commit()
                                        nameEditText.text.clear()
                                        birthdateEditText.text.clear()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "로그인에 실패했습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        } else {
                            Toast.makeText(
                                context,
                                "회원가입에 실패했습니다. 이메일 확인 바람.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(context, "모든 필드를 입력해야합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initSignInOutButton() {

        binding.signInOutButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (auth.currentUser == null) {
                // 로그인
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()){ task ->
                        if(task.isSuccessful){
                            successSignIn()
                            val homeFragment = HomeFragment()
                            activity?.supportFragmentManager?.beginTransaction()
                                ?.replace(R.id.fragmentContainer, homeFragment)
                                ?.commit()
                        }else{
                            Toast.makeText(context, "로그인에 실패했습니다. 이메일, 비밀번호 확인바람.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
            } else {
                //로그 아웃
                auth.signOut()
                binding.emailEditText.text.clear()
                binding.emailEditText.isEnabled = true
                binding.passwordEditText.text.clear()
                binding.passwordEditText.isEnabled = true

                binding.signInOutButton.text = "로그인"
                binding.signInOutButton.isEnabled = false
                binding.signUpButton.isEnabled = false
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if(auth.currentUser == null){
            binding.emailEditText.text.clear()
            binding.emailEditText.isEnabled = true
            binding.passwordEditText.text.clear()
            binding.passwordEditText.isEnabled = true

            binding.signInOutButton.text = "로그인"
            binding.signInOutButton.isEnabled = false
            binding.signUpButton.isEnabled = false
        }else{
            binding.emailEditText.setText(auth.currentUser!!.email)
            binding.emailEditText.isEnabled = false
            binding.passwordEditText.setText("**********")
            binding.passwordEditText.isEnabled = false

            binding.signInOutButton.text = "로그아웃"
            binding.signInOutButton.isEnabled = true
            binding.signUpButton.isEnabled = false
        }
    }

    private fun successSignIn(){
        if(auth.currentUser == null){
            Toast.makeText(context, "로그인에 실패, 다시시도", Toast.LENGTH_SHORT)
                .show()
            return
        }

        binding.emailEditText.isEnabled = false
        binding.passwordEditText.isEnabled = false
        binding.signUpButton.isEnabled = false
        binding.signInOutButton.text = "로그아웃"
    }
}