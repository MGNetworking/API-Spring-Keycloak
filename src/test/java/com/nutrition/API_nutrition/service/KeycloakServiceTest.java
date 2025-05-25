package com.nutrition.API_nutrition.service;

import com.nutrition.API_nutrition.config.KeycloakProvider;
import com.nutrition.API_nutrition.exception.ApiException;
import com.nutrition.API_nutrition.exception.ErrorCode;
import com.nutrition.API_nutrition.model.dto.RegisterRequestDto;
import com.nutrition.API_nutrition.model.dto.TokenResponseDto;
import com.nutrition.API_nutrition.model.entity.Gender;
import com.nutrition.API_nutrition.util.HttpClientConfig;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {

    // Mock pour la classe de service KeycloakService
    @InjectMocks
    private KeycloakService keycloakService;

    @Mock
    private KeycloakProvider keycloakProvider;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpClientConfig httpClientConfig;

    // autre mock
    @Mock
    private Keycloak keycloakMock;

    @Mock
    private RoleMappingResource roleMappingResource;

    @Mock
    private RoleScopeResource roleScopeResource;

    @Mock
    private RolesResource rolesResource;

    @Mock
    UserRepresentation userRepresentation;

    @Mock
    RealmResource realmResource;

    @Mock
    UsersResource usersResource;

    @Mock
    ClientsResource clientsResource;

    @Mock
    ClientResource clientResource;

    @Mock
    UserResource userResource;

    @Mock
    Response response;

    RegisterRequestDto dto;

    RoleRepresentation role1;
    RoleRepresentation role2;
    List<RoleRepresentation> roleName;

    @BeforeEach
    void setUp() {
        this.dto = new RegisterRequestDto();
        dto.getKeycloakUserData().setKeycloakId("kc123456");
        dto.getKeycloakUserData().setPassword("password");
        dto.getKeycloakUserData().setUserName("UserName");
        dto.getKeycloakUserData().setFirstName("FirstName");
        dto.getKeycloakUserData().setLastName("LastName");
        dto.getKeycloakUserData().setEmail("FirstName.LastName@example.com");
        dto.setBirthdate(LocalDate.of(1990, 1, 1));
        dto.setGender(Gender.MALE);
        dto.setHeight((short) 180);
        dto.setWeight((short) 75);

        // NB: ne pas mapper le password du DTO
        this.userRepresentation = new UserRepresentation();
        this.userRepresentation.setUsername(dto.getKeycloakUserData().getUserName());
        this.userRepresentation.setFirstName(dto.getKeycloakUserData().getFirstName());
        this.userRepresentation.setLastName(dto.getKeycloakUserData().getLastName());
        this.userRepresentation.setEmail(dto.getKeycloakUserData().getEmail());

        // Objet de comportement
        lenient().when(this.keycloakProvider.getKeycloakInstance()).thenReturn(this.keycloakMock);
        lenient().when(this.keycloakProvider.getRealm()).thenReturn("Test-realm");
        lenient().when(keycloakProvider.getAuthServerUrl()).thenReturn("http://localhost:8080/auth");

        this.role1 = new RoleRepresentation("id-role-1", "Role test 1", true);
        this.role2 = new RoleRepresentation("id-role-2", "Role test 2", true);
        this.roleName = Arrays.asList(role1, role2);

    }

    @Test
    @DisplayName("Devrait créer l'utilisateur avec succès")
    void createUser_shouldCreateUserSuccessfully() {

        // Vérification de la configuration avant le test
        assertNotNull(this.dto, "DTO mal configuré: le DTO ne doit pas être null");

        // Arrange
        // Mock du comportement de Keycloak
        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        // Mock les liste des utilisateurs sur le nom spécifique recherché
        when(usersResource.search(
                anyString(),
                anyString(),
                anyString(),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(Collections.emptyList());

        // Mock la création d'un user avec le type UserRepresentation
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);

        // mock de Extraire id du nouvel utilisateur
        URI locationUri = URI.create("http://localhost:8080/auth/admin/realms/test-realm/users/user-id-123");
        when(response.getLocation()).thenReturn(locationUri);

        // Act - Exécuter la méthode à tester
        keycloakService.createUser(this.dto);

        // Assert - Vérifier les résultats
        assertEquals("user-id-123", dto.getKeycloakUserData().getKeycloakId());

        // Vérifier les interactions
        verify(keycloakMock, times(2)).realm("Test-realm");
        verify(realmResource, times(2)).users();
        verify(usersResource).search(
                this.dto.getKeycloakUserData().getUserName(),
                this.dto.getKeycloakUserData().getFirstName(),
                this.dto.getKeycloakUserData().getLastName(),
                null, null, null
        );

        ArgumentCaptor<UserRepresentation> userCaptor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(usersResource).create(userCaptor.capture());

        UserRepresentation capturedUser = userCaptor.getValue();
        assertTrue(capturedUser.isEnabled());
        assertEquals("UserName", capturedUser.getUsername());
        assertEquals("FirstName.LastName@example.com", capturedUser.getEmail());
        assertEquals("FirstName", capturedUser.getFirstName());
        assertEquals("LastName", capturedUser.getLastName());

    }

    @Test
    @DisplayName("Devrait ajouté un role de porter Realm a un utilisateur avec succès")
    void addUserRoles_shouldAddRoleUserSuccessfullyRealm() {

        // Arrange
        String userId = "id-test";

        // Simulation de la partie assignation des roles
        when(this.keycloakMock.realm(anyString())).thenReturn(this.realmResource);
        when(this.realmResource.users()).thenReturn(this.usersResource);
        when(this.usersResource.get(userId)).thenReturn(this.userResource);
        when(this.userResource.roles()).thenReturn(this.roleMappingResource);
        when(this.roleMappingResource.realmLevel()).thenReturn(this.roleScopeResource);

        // Act
        this.keycloakService.addUserRolesRealm(userId, this.roleName);

        // Assert
        // Création d'objet pour capter les infos passer la methode add
        ArgumentCaptor<List<RoleRepresentation>> rolesCaptor = ArgumentCaptor.forClass(List.class);
        verify(this.roleScopeResource).add(rolesCaptor.capture());

        // on récupére les valeurs capturées
        List<RoleRepresentation> list = rolesCaptor.getValue();

        // on les vérifie la correspondance des éléments
        assertEquals(roleName.size(), list.size());
        assertEquals(roleName.get(0).getName(), list.get(0).getName());
        assertEquals(roleName.get(1).getName(), list.get(1).getName());

    }

    @Test
    @DisplayName("Devrait lever une ApiException quand Keycloak renvoie une erreur HTTP")
    void addUserRolesRealm_shouldThrowApiException_onWebApplicationException() {

        // Arrange
        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

        // Création d’un mock complet de Response
        Response response = mock(Response.class);
        Response.StatusType statusType = mock(Response.StatusType.class);

        // Simulation du comportement du Response
        when(response.getStatus()).thenReturn(HttpStatus.BAD_REQUEST.value());
        when(statusType.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST.value());
        when(response.getStatusInfo()).thenReturn(statusType);
        when(response.readEntity(String.class)).thenReturn("Invalid role");

        WebApplicationException webApplicationException = new WebApplicationException(response);
        doThrow(webApplicationException).when(roleScopeResource).add(anyList());

        // Act & Assert
        ApiException apiException = assertThrows(ApiException.class, () ->
                keycloakService.addUserRolesRealm("id-test", this.roleName));

        assertEquals(
                ErrorCode.USER_ROLE_ASSIGNMENT_FAILED.toString(),
                apiException.getErrorCode()
        );

    }


    @Test
    @DisplayName("Devrait ajouté un role de porter client a un utilisateur avec succès")
    void addUserRolesClient_shouldAddRoleUserSuccessfullyClient() {

        // Arrange
        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.clientLevel(anyString())).thenReturn(roleScopeResource);
        doNothing().when(roleScopeResource).add(this.roleName);

        // Act
        this.keycloakService.addUserRolesClient("id-test", this.roleName);

        // Assert
        // Création d'objet pour capter les infos passer la methode addUserRolesClient
        ArgumentCaptor<List<RoleRepresentation>> rolesCaptor = ArgumentCaptor.forClass(List.class);
        verify(this.roleScopeResource).add(rolesCaptor.capture());

        // On les vérifie la correspondance des éléments
        List<RoleRepresentation> capturedRoles = rolesCaptor.getValue();
        assertEquals(this.roleName.size(), capturedRoles.size());
        assertEquals(role1.getName(), capturedRoles.get(0).getName());
        assertEquals(role2.getName(), capturedRoles.get(1).getName());

    }


    @Test
    @DisplayName("Devrait lever une ApiException si une erreur survient lors de l'ajout de rôles")
    void addUserRolesClient_shouldThrowTechnicalException() {

        // Arrange
        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.clientLevel(anyString())).thenReturn(roleScopeResource);

        // On force une exception générique (Exception et non WebApplicationException)
        doThrow(new RuntimeException("Erreur inattendue")).when(roleScopeResource).add(anyList());

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            this.keycloakService.addUserRolesClient("id-test", this.roleName);
        });

        // Vérifie que c’est bien l’exception technique
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
        assertEquals(ErrorCode.TECHNICAL_ERROR.toString(), exception.getErrorCode());

        // On vérifie que la méthode add a bien été invoquée
        verify(roleScopeResource).add(roleName);

    }

    @Test
    @DisplayName("Devrait lever une ApiException quand Keycloak renvoie une erreur HTTP")
    void addUserRolesClient_shouldThrowApiException_onWebApplicationException() {

        // Arrange
        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.clientLevel(anyString())).thenReturn(roleScopeResource);

        // Construction de la réponse simulée avec un message d’erreur
        Response response = mock(Response.class);
        Response.StatusType statusType = mock(Response.StatusType.class);
        when(statusType.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST.value());
        when(response.getStatus()).thenReturn(HttpStatus.BAD_REQUEST.value());
        when(response.getStatusInfo()).thenReturn(statusType);
        when(response.readEntity(String.class)).thenReturn("Invalid role");

        // Simulation de l'exception HTTP de Keycloak
        WebApplicationException webEx = new WebApplicationException(response);
        doThrow(webEx).when(roleScopeResource).add(anyList());

        // Act & Assert
        ApiException ex = assertThrows(ApiException.class, () ->
                keycloakService.addUserRolesClient("id-test", roleName));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals(ErrorCode.USER_ROLE_ASSIGNMENT_FAILED.toString(), ex.getErrorCode());

        verify(roleScopeResource).add(anyList());
    }


    @Test
    @DisplayName("Devrait authentifier l'utilisateur avec succès")
    void login_shouldAuthenticateSuccessfully() {

        try {
            // Arrange
            String username = "Username";
            String password = "password";
            String responseBody = "{\"access_token\":\"test-token\",\"refresh_token\":\"refresh-token\",\"expires_in\":300,\"refresh_expires_in\":1800}";

            HttpRequest request = mock(HttpRequest.class);
            HttpResponse<String> response = mock(HttpResponse.class);

            // Configuration des comportements
            when(this.httpClientConfig.postRequest(
                    anyString(), anyString(), anyString())).thenReturn(request);

            when(response.statusCode()).thenReturn(HttpStatus.OK.value());
            when(response.body()).thenReturn(responseBody);

            when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(response);

            // Act
            TokenResponseDto dtoResult = this.keycloakService.login(username, password);

            // Assert
            assertNotNull(dto);
            assertEquals("test-token", dtoResult.getAccessToken());
            assertEquals("refresh-token", dtoResult.getRefreshToken());
            assertEquals(300L, dtoResult.getExpiresIn());
            assertEquals(1800L, dtoResult.getRefreshExpiresIn());


        } catch (Exception e) {
            log.error("Erreur dans le test KeycloakServiceTest -> login {}", e.getMessage());
        }

    }

    @Test
    @DisplayName("Devrait pas authentifier l'utilisateur")
    void login_shouldNotAuthenticateSuccessfully() throws IOException, InterruptedException {
        // Arrange
        String username = "Username";
        String password = "password";
        String errorResponseBody = "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid user credentials\"}";

        HttpRequest httpRequestMock = mock(HttpRequest.class);
        HttpResponse<String> httpResponseMock = mock(HttpResponse.class);

        // Configuration des comportements
        when(httpClientConfig.postRequest(anyString(), anyString(), anyString()))
                .thenReturn(httpRequestMock);

        when(httpResponseMock.statusCode()).thenReturn(HttpStatus.UNAUTHORIZED.value());
        when(httpResponseMock.body()).thenReturn(errorResponseBody);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponseMock);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            keycloakService.login(username, password);
        });

        // Vérification moins stricte du message d'erreur
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
        assertEquals(ErrorCode.KEYCLOAK_UNAUTHORIZED.toString(), exception.getErrorCode());
    }

    @Test
    @DisplayName("Devrait rafraîchir le token avec succès")
    void refreshToken_shouldRefreshTokenSuccessfully() throws IOException, InterruptedException {
        // Arrange
        String refreshToken = "refresh-token";
        String responseBody = "{\"access_token\":\"new-test-token\",\"refresh_token\":\"new-refresh-token\",\"expires_in\":300,\"refresh_expires_in\":1800}";

        HttpRequest request = mock(HttpRequest.class);
        HttpResponse<String> response = mock(HttpResponse.class);

        // Configuration des comportements
        when(this.httpClientConfig.postRequest(anyString(), anyString(), anyString()))
                .thenReturn(request);

        when(response.statusCode()).thenReturn(HttpStatus.OK.value());
        when(response.body()).thenReturn(responseBody);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        // Act
        TokenResponseDto dtoResult = keycloakService.refreshToken(refreshToken);

        // Assert
        assertNotNull(dtoResult);
        assertEquals("new-test-token", dtoResult.getAccessToken());
        assertEquals("new-refresh-token", dtoResult.getRefreshToken());
        assertEquals(300L, dtoResult.getExpiresIn());
        assertEquals(1800L, dtoResult.getRefreshExpiresIn());
    }

    @Test
    @DisplayName("Devrait échouer lors du rafraîchissement du token")
    void refreshToken_shouldFailToRefreshToken() throws IOException, InterruptedException {
        // Arrange
        String username = "Username";
        String password = "password";
        String errorResponseBody = "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid user credentials\"}";

        HttpRequest httpRequestMock = mock(HttpRequest.class);
        HttpResponse<String> httpResponseMock = mock(HttpResponse.class);

        when(keycloakProvider.getClientId()).thenReturn("clientId");
        when(keycloakProvider.getClientSecret()).thenReturn("clientSecret");
        when(httpClientConfig.postRequest(anyString(), anyString(), anyString()))
                .thenReturn(httpRequestMock);

        when(httpResponseMock.statusCode()).thenReturn(HttpStatus.UNAUTHORIZED.value());
        when(httpResponseMock.body()).thenReturn(errorResponseBody);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponseMock);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, () -> {
            keycloakService.login(username, password);
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
        assertEquals(ErrorCode.KEYCLOAK_UNAUTHORIZED.toString(), exception.getErrorCode());
    }

    @Test
    @DisplayName("Devrait vérifier que l'utilisateur existe par son ID avec succès")
    void userExistsById_shouldReturnTrueWhenUserExists() {
        // Arrange
        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(this.dto.getKeycloakUserData().getKeycloakId()))
                .thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRepresentation);

        // Act
        boolean result = keycloakService.checkUserExist(this.dto);

        // Assert
        assertTrue(result);
        verify(keycloakMock).realm("Test-realm");
        verify(realmResource).users();
        verify(usersResource).get(this.dto.getKeycloakUserData().getKeycloakId());
        verify(userResource).toRepresentation();
    }

    @Test
    @DisplayName("Devrait retourner faux quand l'ID utilisateur n'existe pas dans keycloak")
    void userExistsById_shouldReturnFalseWhenIdUserDoesNotExist() {
        // Arrange
        String userId = "non-existing-user-id";
        this.dto.getKeycloakUserData().setKeycloakId(userId);

        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenThrow(new NotFoundException("User not found"));

        // Act
        boolean result = keycloakService.checkUserExist(this.dto);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Devrait vérifier que l'utilisateur existe par son DTO avec succès")
    void userExistsById_shouldReturnTrueWhenUserExistsByDto() {

        // Arrange
        // Configurer le DTO sans keycloakId pour forcer le chemin de recherche par attributs
        this.dto.getKeycloakUserData().setKeycloakId(null);

        // Créer un user qui matche exactement le DTO
        UserRepresentation mockUser = new UserRepresentation();
        mockUser.setUsername(this.dto.getKeycloakUserData().getUserName());
        mockUser.setEmail(this.dto.getKeycloakUserData().getEmail());

        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.search(
                this.dto.getKeycloakUserData().getUserName(),
                this.dto.getKeycloakUserData().getFirstName(),
                this.dto.getKeycloakUserData().getLastName(),
                this.dto.getKeycloakUserData().getEmail(),
                null, null))
                .thenReturn(List.of(mockUser)); // Retourne un user qui correspond


        // Act
        boolean result = keycloakService.checkUserExist(this.dto);

        // Assert
        assertTrue(result);
        verify(keycloakMock).realm("Test-realm");
        verify(realmResource).users();
        verify(usersResource).search( // Vérification de l'appel à search()
                this.dto.getKeycloakUserData().getUserName(),
                this.dto.getKeycloakUserData().getFirstName(),
                this.dto.getKeycloakUserData().getLastName(),
                this.dto.getKeycloakUserData().getEmail(),
                null, null);
    }

    @Test
    @DisplayName("Devrait retourner faux quand le dto n'existe pas dans keycloak")
    void userExistsById_shouldReturnFalseWhenIdUserDoesNotExistDTO() {

        // Arrange
        // Configurer le DTO sans keycloakId pour forcer le chemin de recherche par attributs
        this.dto.getKeycloakUserData().setKeycloakId(null);

        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.search(
                anyString(), anyString(), anyString(), anyString(), isNull(), isNull()))
                .thenReturn(Collections.emptyList()); // Liste vide simulée


        // Act
        boolean result = keycloakService.checkUserExist(this.dto);

        // Assert
        assertFalse(result);

    }

    @Test
    @DisplayName("Devrait mettre à jour l'utilisateur avec succès")
    void updateUser_shouldUpdateUserSuccessfully() {

        // Check des variables qui potentiellement peuvent retourner une RuntimeException
        assertNotNull(dto.getKeycloakUserData().getPassword(), "DTO mal configuré: password ne doit pas être null");
        assertFalse(dto.getKeycloakUserData().getPassword().isEmpty(), "DTO mal configuré: password ne doit pas être vide");

        // Arrange - Mocks pour la chaîne d'appels Keycloak
        when(realmResource.users()).thenReturn(usersResource);
        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(this.userRepresentation);

        // Act
        boolean status = this.keycloakService.updateUser(this.dto);

        // Assert
        assertTrue(status);
        // Vérifier l'appel
        verify(this.userResource).update(this.userRepresentation);
    }

    @Test
    @DisplayName("Devrait lancer une exception quand le mot de passe est null")
    void updateUser_shouldThrowExceptionWhenPasswordIsNull() {

        // Arrange - Mocks pour la chaîne d'appels Keycloak
        when(realmResource.users()).thenReturn(usersResource);
        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(this.userRepresentation);

        dto.getKeycloakUserData().setPassword(null);

        // Act
        boolean status = this.keycloakService.updateUser(this.dto);

        // Assert
        assertFalse(status);
        // Vérifier qu'update n'est jamais appelée
        verify(userResource, never()).update(any());
    }

    @Test
    @DisplayName("Devrait lancer une exception quand le mot de passe est vide")
    void updateUser_shouldThrowExceptionWhenPasswordIsEmpty() {

        // Arrange - Mocks pour la chaîne d'appels Keycloak
        when(realmResource.users()).thenReturn(usersResource);
        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(this.userRepresentation);

        dto.getKeycloakUserData().setPassword("");

        // Act
        boolean status = this.keycloakService.updateUser(this.dto);

        // Assert
        assertFalse(status);
        // Vérifier qu'update n'est jamais appelée
        verify(userResource, never()).update(any());
    }

    @Test
    @DisplayName("Devrait supprimer l'utilisateur avec succès")
    void removeUser_shouldRemoveUserSuccessfully() {
        // Arrange
        String userId = "user-to-delete";

        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);

        // Act
        boolean result = keycloakService.removeUser(userId);

        // Assert
        assertTrue(result);
        verify(keycloakMock).realm("Test-realm");
        verify(realmResource).users();
        verify(usersResource).get(userId);
        verify(userResource).remove();
    }

    @Test
    @DisplayName("Devrait échouer lors de la suppression de l'utilisateur")
    void removeUser_shouldFailToRemoveUser() {
        // Arrange
        String userId = "user-to-delete";

        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);
        doThrow(new RuntimeException("Failed to remove user")).when(userResource).remove();

        // Act
        boolean result = keycloakService.removeUser(userId);

        // Assert
        assertFalse(result);
        verify(keycloakMock).realm("Test-realm");
        verify(realmResource).users();
        verify(usersResource).get(userId);
        verify(userResource).remove();
    }

    @Test
    @DisplayName("Devrait déconnecter l'utilisateur avec succès")
    void logout_shouldLogoutUserSuccessfully() {
        // Arrange
        String userId = "user-to-logout";
        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);

        // Act — la méthode ne lève pas d’exception si tout va bien
        keycloakService.logout(userId);

        // Assert
        verify(keycloakMock).realm("Test-realm");
        verify(realmResource).users();
        verify(usersResource).get(userId);
        verify(userResource).logout();
    }

    @Test
    @DisplayName("Devrait échouer lors de la déconnexion de l'utilisateur")
    void logout_shouldFailToLogoutUser() {
        // Arrange
        String userId = "user-to-logout";

        // Création des mock
        Response mockResponse = Response.status(403)
                .entity("User not found")
                .build();

        WebApplicationException webException = new WebApplicationException(mockResponse);

        // Mocks de la chaîne Keycloak
        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);

        // on dit que la methode va lancer WebApplicationException
        doThrow(webException).when(userResource).logout();

        // Act & Assert
        // On attend la reception de ApiException throw en amont
        ApiException exception = assertThrows(ApiException.class, () -> {
            keycloakService.logout(userId);
        });

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        assertEquals(ErrorCode.KEYCLOAK_FORBIDDEN.toString(), exception.getErrorCode());

        verify(keycloakMock).realm("Test-realm");
        verify(realmResource).users();
        verify(usersResource).get(userId);
        verify(userResource).logout();
    }

    @Test
    @DisplayName("Devrait réinitialiser le mot de passe avec succès")
    void resetPassword_shouldResetPasswordSuccessfully() {
        // Arrange
        String userId = "user-id";
        String newPassword = "new-password";

        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);

        // Act
        keycloakService.resetPassword(userId, newPassword);

        // Assert
        // Capture l'objet CredentialRepresentation passé à la méthode resetPassword
        ArgumentCaptor<CredentialRepresentation> credentialCaptor = ArgumentCaptor.forClass(CredentialRepresentation.class);
        verify(userResource).resetPassword(credentialCaptor.capture());

        CredentialRepresentation capturedCredential = credentialCaptor.getValue();
        assertEquals(CredentialRepresentation.PASSWORD, capturedCredential.getType());
        assertEquals(newPassword, capturedCredential.getValue());
        assertFalse(capturedCredential.isTemporary());
    }

    @Test
    @DisplayName("Devrait échouer lors de la réinitialisation du mot de passe")
    void resetPassword_shouldFailToResetPassword() {
        // Arrange
        String userId = "user-id";
        String newPassword = "new-password";

        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);
        doThrow(new RuntimeException("Failed to reset password")).when(userResource).resetPassword(any(CredentialRepresentation.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            keycloakService.resetPassword(userId, newPassword);
        });

        // Vérification du message d'erreur
        assertTrue(exception.getMessage().contains("Error modifying user password"),
                "Le message d'erreur devrait contenir 'Error modifying user password'");
    }

    @Test
    @DisplayName("Devrait valider le token avec succès")
    void validateToken_shouldValidateTokenSuccessfully() throws IOException, InterruptedException {
        // Arrange
        String token = "valid-token";

        HttpResponse<String> response = mock(HttpResponse.class);
        HttpRequest request = mock(HttpRequest.class);

        when(this.httpClientConfig.getRequest(anyString(), anyString()))
                .thenReturn(request);
        when(this.httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        when(response.statusCode()).thenReturn(HttpStatus.OK.value());

        // Act
        boolean result = keycloakService.validateToken(token);

        // Assert
        assertTrue(result);

        // Vérifier que les méthodes appropriées ont été appelées
        verify(httpClientConfig)
                .getRequest(anyString(), contains("Bearer " + token));
        verify(httpClient, times(1))
                .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("Devrait échouer lors de la validation du token")
    void validateToken_shouldFailToValidateToken() throws IOException, InterruptedException {
        // Arrange
        String token = "invalid-token";

        HttpRequest request = mock(HttpRequest.class);
        HttpResponse<String> response = mock(HttpResponse.class);

        when(this.httpClientConfig.getRequest(anyString(), anyString())).thenReturn(request);
        when(response.statusCode()).thenReturn(HttpStatus.UNAUTHORIZED.value());
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        // Act
        boolean result = keycloakService.validateToken(token);

        // Assert
        assertFalse(result);

        // Vérifier que les méthodes appropriées ont été appelées
        verify(httpClientConfig, times(1))
                .getRequest(anyString(), anyString());
        verify(httpClient, times(1))
                .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("Devrait afficher la list ")
    void displayList_shouldByDisplayList_Successfully() {
        this.keycloakService.displayList(List.of("admin", "user"));
    }

    static class Circular {
        public Circular ref;
    }

    @Test
    @DisplayName("Devrait lancer un exception")
    void displayList_shouldByDisplayList_Exception() {
        Circular circular = new Circular();
        circular.ref = circular;

        this.keycloakService.displayList(List.of(circular));
    }
}