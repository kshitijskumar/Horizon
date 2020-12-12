package com.example.horizon.viewmodel

import android.net.Uri
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.example.horizon.models.CurrentUser
import com.example.horizon.models.UploadedPosts
import com.example.horizon.repository.MainRepository
import com.example.horizon.response.*
import com.example.horizon.utils.CurrentUserDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel @ViewModelInject constructor(
        private val repository: MainRepository
) : ViewModel() {

    private val _post = MutableLiveData<UploadedPosts>()
    val post : LiveData<UploadedPosts>
        get() = _post

    private val _userProfile = MutableLiveData<CurrentUser>()
    val userprofile: LiveData<CurrentUser>
        get() = _userProfile

    val allPostsLiveData = Pager(PagingConfig(20)){
        repository.getAllPostsRepository()
    }.flow.cachedIn(viewModelScope).asLiveData(viewModelScope.coroutineContext)

    fun getParticularUserPostsViewModel(userId: String) = Pager(PagingConfig(20)){
        repository.getParticularUserPostsRepository(userId)
    }.flow.cachedIn(viewModelScope).asLiveData(viewModelScope.coroutineContext)

    fun getCurrentUserViewModel() = repository.getCurrentUserRepository()

    fun getCurrentUserDetailsViewModel(userUid: String) =  repository.getCurrentUserDetailsRepository(userUid)

    suspend fun loginUserViewModel(email: String, password: String) = flow {
        emit(LoginResponse.LoginLoading)
        if (email.isEmpty() or password.isEmpty()){
            emit(LoginResponse.LoginError("Email or Password can't be left empty"))
        }else{
            withContext(Dispatchers.IO){
                repository.loginUserRepository(email, password).collect {
                    withContext(Dispatchers.Main){
                        emit(it)
                    }
                }
            }
        }
    }

    suspend fun signUpUserViewModel(name: String, email: String, password: String, confirmPassword: String) = flow {
        emit(SignUpResponse.SignUpLoading)
        when {
            name.isEmpty() or email.isEmpty() or password.isEmpty() or confirmPassword.isEmpty() -> {
                emit(SignUpResponse.SignUpError("None of the fields can be left empty"))
            }
            password != confirmPassword -> {
                Log.d("SignUpViewModel", "password is $password and confirm is $confirmPassword")
                emit(SignUpResponse.SignUpError("Passwords doesn't match"))
            }
            else -> {
                withContext(Dispatchers.IO){
                    repository.signUpNewUserRepository(name, email, password).collect{
                        withContext(Dispatchers.Main){
                            emit(it)
                        }
                    }
                }
            }
        }
    }

    suspend fun uploadNewPostViewModel(title: String, content: String, imgUri: Uri?) = flow {
        emit(PostUploadResponse.PostUploadLoading)
        when {
            title.isEmpty() or content.isEmpty() -> {
                emit(PostUploadResponse.PostUploadError("Title or content can't be left empty"))
            }
            imgUri == null -> {
                emit(PostUploadResponse.PostUploadError("Please upload an image"))
            }
            else -> {
                withContext(Dispatchers.IO){
                    repository.uploadNewPostRepository(imgUri, title, content).collect {
                        withContext(Dispatchers.Main){
                            emit(it)
                        }
                    }
                }
            }
        }
    }

    suspend fun changeUserProfileViewModel(newName: String, newBio: String, imageUri: Uri?, removeProfileImgMsg:String) = flow {
        emit(UserDetailsChanged.ChangeLoading)
        if (newName.isEmpty()){
            emit(UserDetailsChanged.ChangeError("Name cannot be left empty"))
        }else{
            withContext(Dispatchers.IO){
                repository.changeUserProfileRepository(newName, newBio, imageUri, removeProfileImgMsg).collect{
                    withContext(Dispatchers.Main){
                        emit(it)
                    }
                }
            }
        }
    }

    suspend fun getPostViewModel(postId: String) = flow {
        withContext(Dispatchers.IO){
            repository.getPostRepository(postId).collect {
                withContext(Dispatchers.Main){
                    when(it){
                        is PostRetrieveResponse.PostRetrieveSuccessful -> {
                            _post.value = it.post
                            emit(it)
                        }
                        is PostRetrieveResponse.PostRetrieveError -> emit(it)
                        is PostRetrieveResponse.PostRetrieveLoading -> emit(it)
                    }
                }
            }
        }
    }

    fun likeDislikePostViewModel(postId: String, likeDislikeList: ArrayList<String>) = viewModelScope.launch{
        if (likeDislikeList.contains(CurrentUserDetails.userUid)){
            likeDislikeList.remove(CurrentUserDetails.userUid)
            repository.likeDislikePostRepository(postId, likeDislikeList)
        }else{
            likeDislikeList.add(CurrentUserDetails.userUid)
            repository.likeDislikePostRepository(postId,  likeDislikeList)
        }
    }

    fun getAnotherUserDetailsViewModel(userId: String) = flow {
        emit(UserDetailsResponse.LoadingUserDetails)
        withContext(Dispatchers.IO){
            repository.getAnotherUserDetailsRepository(userId).collect{
                withContext(Dispatchers.Main){
                    when(it){
                        is UserDetailsResponse.SuccessUserDetails -> {
                            _userProfile.value = it.userDetails
                            emit(it)
                        }
                        is UserDetailsResponse.ErrorUserDetails -> emit(it)
                        is UserDetailsResponse.LoadingUserDetails -> emit(it)
                    }
                }
            }
        }
    }
}