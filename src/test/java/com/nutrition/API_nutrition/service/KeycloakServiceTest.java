package com.nutrition.API_nutrition.service;

import com.nutrition.API_nutrition.config.KeycloakProvider;
import com.nutrition.API_nutrition.model.dto.RegisterRequestDto;
import com.nutrition.API_nutrition.model.entity.Gender;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {

    @InjectMocks
    private KeycloakService keycloakService;

    @Mock
    private KeycloakProvider keycloakProvider;

    @Mock
    private HttpClient httpClient;

    @Mock
    private Keycloak keycloakMock;

    @Mock
    private RoleMappingResource roleMappingResource;

    @Mock
    private RoleScopeResource roleScopeResource;

    @Mock
    private RolesResource rolesResource;

    RegisterRequestDto dto;
    UserRepresentation userRepresentation;
    RealmResource realmResource;
    UsersResource usersResource;
    UserResource userResource;
    Response response;

    @BeforeEach
    void setUp() {
        this.dto = new RegisterRequestDto();
        dto.setKeycloakId("kc123456");
        dto.setPassword("password");
        dto.setUserName("UserName");
        dto.setFirstName("FirstName");
        dto.setLastName("LastName");
        dto.setEmail("FirstName.LastName@example.com");
        dto.setBirthdate(LocalDate.of(1990, 1, 1));
        dto.setGender(Gender.MALE);
        dto.setHeight((short) 180);
        dto.setWeight((short) 75);

        // NB: ne pas mapper le password du DTO
        this.userRepresentation = new UserRepresentation();
        this.userRepresentation.setUsername(dto.getUserName());
        this.userRepresentation.setFirstName(dto.getFirstName());
        this.userRepresentation.setLastName(dto.getLastName());
        this.userRepresentation.setEmail(dto.getEmail());

        // Objet de comportement
        lenient().when(this.keycloakProvider.getKeycloakInstance()).thenReturn(this.keycloakMock);
        lenient().when(this.keycloakProvider.getRealm()).thenReturn("Test-realm");
        lenient().when(keycloakProvider.getAuthServerUrl()).thenReturn("http://localhost:8080/auth");

        this.realmResource = mock(RealmResource.class);
        this.usersResource = mock(UsersResource.class);
        this.userResource = mock(UserResource.class);
        this.response = mock(Response.class);

    }

    @AfterEach
    void tearDown() {
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
        when(usersResource.search(anyString(), eq(true))).thenReturn(Collections.emptyList());

        // Mock la création d'un user avec le type UserRepresentation
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);

        // mock de Extraire id du nouvel utilisateur
        URI locationUri = URI.create("http://localhost:8080/auth/admin/realms/test-realm/users/user-id-123");
        when(response.getLocation()).thenReturn(locationUri);

        // Act - Exécuter la méthode à tester
        keycloakService.createUser(this.dto);

        // Assert - Vérifier les résultats
        assertEquals("user-id-123", dto.getKeycloakId());

        // Vérifier les interactions
        verify(keycloakMock, times(2)).realm("Test-realm");
        verify(realmResource, times(2)).users();
        verify(usersResource).search(this.dto.getUserName(), true);

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
    void addUserRoles_shouldAddRoleUserSuccessfully() {

        // Arrange
        String userId = "id-test";
        String role1 = "role1", role2 = "role2";
        List<String> roleName = Arrays.asList(role1, role2);

        // simule la recherche et récupération de la liste des role
        RoleResource roleResource1 = mock(RoleResource.class);
        RoleResource roleResource2 = mock(RoleResource.class);

        RoleRepresentation roleRep1 = new RoleRepresentation();
        RoleRepresentation roleRep2 = new RoleRepresentation();
        roleRep1.setName(role1);
        roleRep2.setName(role2);

        when(this.rolesResource.get(role1)).thenReturn(roleResource1);
        when(this.rolesResource.get(role2)).thenReturn(roleResource2);
        when(this.realmResource.roles()).thenReturn(this.rolesResource);

        when(roleResource1.toRepresentation()).thenReturn(roleRep1);
        when(roleResource2.toRepresentation()).thenReturn(roleRep2);

        // Simulation de la partie assignation des roles
        when(this.keycloakMock.realm(anyString())).thenReturn(this.realmResource);
        when(this.realmResource.users()).thenReturn(this.usersResource);
        when(this.usersResource.get(userId)).thenReturn(this.userResource);
        when(this.userResource.roles()).thenReturn(this.roleMappingResource);
        when(this.roleMappingResource.realmLevel()).thenReturn(this.roleScopeResource);

        // Act
        this.keycloakService.addUserRoles(userId, roleName);

        // Assert
        // Création d'objet pour capter les infos passer la methode add
        ArgumentCaptor<List<RoleRepresentation>> rolesCaptor = ArgumentCaptor.forClass(List.class);
        verify(this.roleScopeResource).add(rolesCaptor.capture());

        // on récupére les valeurs capturées
        List<RoleRepresentation> list = rolesCaptor.getValue();

        // on les vérifie la correspondance des éléments
        assertEquals(roleName.size(), list.size());
        assertEquals(roleName.get(0), list.get(0).getName());
        assertEquals(roleName.get(1), list.get(1).getName());

    }

    @Test
    void login() {
    }

    @Test
    void refreshToken() {

    }

    @Test
    void userExistsById() {
    }

    @Test
    @DisplayName("Devrait mettre à jour l'utilisateur avec succès")
    void updateUser_shouldUpdateUserSuccessfully() {

        // Check des variables qui potentiellement peuvent retourner une RuntimeException
        assertNotNull(dto.getPassword(), "DTO mal configuré: password ne doit pas être null");
        assertFalse(dto.getPassword().isEmpty(), "DTO mal configuré: password ne doit pas être vide");

        // Arrange - Mocks pour la chaîne d'appels Keycloak
        when(realmResource.users()).thenReturn(usersResource);
        when(keycloakMock.realm(anyString())).thenReturn(realmResource);
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(this.userRepresentation);

        // Act
        boolean status = this.keycloakService.updateUser(this.dto);

        // Assert
        assertTrue(status);

        // Vérifier qu'update a été appelé avec l'objet UserRepresentation
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

        dto.setPassword(null);

        // Act
        boolean status = this.keycloakService.updateUser(this.dto);

        // Assert
        assertFalse(status);
        verify(userResource, never()).update(any());
    }

    @Test
    void removeUser() {
    }

    @Test
    void logout() {
    }

    @Test
    void resetPassword() {
    }

    @Test
    void validateToken() {
    }
}