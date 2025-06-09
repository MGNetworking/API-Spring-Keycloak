package com.nutrition.API_nutrition.service;

import com.nutrition.API_nutrition.config.KeycloakProvider;
import com.nutrition.API_nutrition.exception.ApiException;
import com.nutrition.API_nutrition.exception.ErrorCode;
import com.nutrition.API_nutrition.model.dto.UserInputDTO;
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
import org.keycloak.representations.idm.ClientRepresentation;
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

    UserInputDTO dto;

    RoleRepresentation role1;
    RoleRepresentation role2;
    List<RoleRepresentation> roleName;

    @BeforeEach
    void setUp() {
        this.dto = new UserInputDTO();
        dto.setBirthdate(LocalDate.of(1990, 1, 1));
        dto.setGender(Gender.MALE);
        dto.setHeight((short) 180);
        dto.setWeight((short) 75);

        // NB: ne pas mapper le password du DTO
        this.userRepresentation = new UserRepresentation();

        // Objet de comportement
        lenient().when(this.keycloakProvider.getKeycloakInstance()).thenReturn(this.keycloakMock);
        lenient().when(this.keycloakProvider.getRealm()).thenReturn("Test-realm");
        lenient().when(keycloakProvider.getAuthServerUrl()).thenReturn("http://localhost:8080/auth");

        this.role1 = new RoleRepresentation("id-role-1", "Role test 1", true);
        this.role2 = new RoleRepresentation("id-role-2", "Role test 2", true);
        this.roleName = Arrays.asList(role1, role2);

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
        String clientUuid = "client-uuid-123";
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setId( clientUuid);

        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.clients()).thenReturn(clientsResource);
        when(keycloakProvider.getClientId()).thenReturn(clientUuid);
        when(clientsResource.findByClientId(anyString())).thenReturn(List.of(clientRep));

        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.clientLevel(clientUuid)).thenReturn(roleScopeResource);
        doNothing().when(roleScopeResource).add(this.roleName);

        // Act
        this.keycloakService.addUserRolesClient(clientUuid, this.roleName);

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
        String clientUuid = "client-uuid-123";
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setId( clientUuid);

        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.clients()).thenReturn(clientsResource);
        when(keycloakProvider.getClientId()).thenReturn(clientUuid);
        when(clientsResource.findByClientId(anyString())).thenReturn(List.of(clientRep));

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
        String clientUuid = "client-uuid-123";
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setId( clientUuid);

        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(realmResource.clients()).thenReturn(clientsResource);
        when(keycloakProvider.getClientId()).thenReturn(clientUuid);
        when(clientsResource.findByClientId(anyString())).thenReturn(List.of(clientRep));

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