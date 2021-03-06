package org.jellyfin.androidtv.ui.startup

import androidx.lifecycle.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.auth.ServerRepository
import org.jellyfin.androidtv.auth.model.ConnectedState
import org.jellyfin.androidtv.auth.model.LoginState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.User
import java.util.*

class LoginViewModel(
	private val serverRepository: ServerRepository,
	private val authenticationRepository: AuthenticationRepository,
) : ViewModel() {
	val discoveredServers: Flow<Server>
		get() = serverRepository.getDiscoveryServers()

	private val _storedServers = MutableLiveData<List<Server>>()
	val storedServers: LiveData<List<Server>>
		get() = _storedServers

	init {
		// Initial values
		viewModelScope.launch {
			_storedServers.postValue(serverRepository.getStoredServers())
		}
	}

	suspend fun getServer(id: UUID) = serverRepository.getStoredServers()
		.find { it.id == id }

	suspend fun getUsers(server: Server) = serverRepository.gerServerUsers(server)

	fun addServer(address: String) = liveData {
		serverRepository.addServer(address).onEach {
			// Reload stored servers when new server is added
			if (it is ConnectedState) _storedServers.postValue(serverRepository.getStoredServers())

			emit(it)
		}.collect()
	}

	fun authenticate(user: User, server: Server): LiveData<LoginState> = authenticationRepository.authenticateUser(user, server).asLiveData()

	fun login(server: Server, username: String, password: String): LiveData<LoginState> = authenticationRepository.login(server, username, password).asLiveData()

	fun getUserImage(server: Server, user: User): String? = authenticationRepository.getUserImageUrl(server, user)
}
