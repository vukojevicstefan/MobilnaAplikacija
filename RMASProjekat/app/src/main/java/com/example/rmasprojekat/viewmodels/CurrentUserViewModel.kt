package com.example.rmasprojekat.viewmodels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.rmasprojekat.data.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CurrentUserViewModel : ViewModel() {
    private val _currentUser = MutableLiveData<User>()
    val currentUser: LiveData<User> = _currentUser
    fun setCurrentUser(user: User) {
        _currentUser.value = user
    }
}