package com.nutrition.API_nutrition.controller;

import com.nutrition.API_nutrition.model.response.GenericApiResponse;
import com.nutrition.API_nutrition.service.KeycloakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    @Mock
    private KeycloakService keycloakService;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("getListRolesRealm: récupérer la list des Role du Domain")
    public void testGetListRolesRealm() {
        // Arrange
        List<RoleRepresentation> mockRoles = Arrays.asList(new RoleRepresentation(), new RoleRepresentation());
        when(keycloakService.getRealmScopedRoles()).thenReturn(mockRoles);

        // Act
        ResponseEntity<GenericApiResponse<List<RoleRepresentation>>> response = adminController
                .getListRolesRealm();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(mockRoles);
        verify(keycloakService, times(1)).getRealmScopedRoles();
    }

    @Test
    @DisplayName("getListRolesClient: récupérer la list des Role client")
    public void testGetListRolesClient() {
        // Arrange
        List<RoleRepresentation> mockRoles = Arrays.asList(new RoleRepresentation(), new RoleRepresentation());
        when(keycloakService.getClientScopedRoles()).thenReturn(mockRoles);

        // Act
        ResponseEntity<GenericApiResponse<List<RoleRepresentation>>> response = adminController
                .getListRolesClient();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(mockRoles);
        verify(keycloakService, times(1)).getClientScopedRoles();
    }

    @Test
    @DisplayName("addUserRolesRealm: devrait valider l'ajout de la list des rôles domain")
    public void testAddUserRolesRealm_validInput() {
        // Arrange
        String userId = "user123";
        List<RoleRepresentation> roles = Arrays.asList(new RoleRepresentation());

        // Créez un spy pour la methode interne
        AdminController adminControllerSpy = Mockito.spy(adminController);

        // Puis mockez la méthode interne
        when(adminControllerSpy.validateUserIdAndRoles(userId, roles))
                .thenReturn(Optional.empty());

        // On mock le service pour éviter un appel réel
        doNothing().when(keycloakService).addUserRolesRealm(userId, roles);

        // Act
        ResponseEntity<GenericApiResponse<Void>> response = adminController
                .addUserRolesRealm(userId, roles);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("The roles have been successfully assigned");
        verify(keycloakService, times(1))
                .addUserRolesRealm(userId, roles);
    }

    @Test
    @DisplayName("addUserRolesRealm: devrait échouer car userId est vide")
    void addUserRolesRealm_ShouldReturnBadRequest_WhenUserIdIsEmpty() {
        // Arrange
        String userId = ""; // Simuler un userId vide
        List<RoleRepresentation> roles = Arrays.asList(new RoleRepresentation());

        // Act
        ResponseEntity<GenericApiResponse<Void>> response =
                adminController.addUserRolesRealm(userId, roles);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("The user ID is missing");

        // Vérifie que le service n'est PAS appelé (validation échouée avant l'appel)
        verifyNoInteractions(keycloakService);
    }

    @Test
    @DisplayName("addUserRolesRealm: devrait échouer car la liste des rôles est vide")
    void addUserRolesRealm_ShouldReturnBadRequest_WhenRolesListIsEmpty() {
        // Arrange
        String userId = "user123";
        List<RoleRepresentation> roles = Collections.emptyList(); // Simuler une liste vide

        // Act
        ResponseEntity<GenericApiResponse<Void>> response =
                adminController.addUserRolesRealm(userId, roles);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("The list of roles cannot be empty.");

        // Vérifie que le service n'est PAS appelé (validation échouée avant l'appel)
        verifyNoInteractions(keycloakService);
    }


    @Test
    @DisplayName("addUserRolesClient: devrait valider l'ajout de la list des rôles Client")
    public void testAddUserRolesClient_validInput() {
        // Arrange
        String userId = "user123";
        List<RoleRepresentation> roles = Arrays.asList(new RoleRepresentation());

        // Act
        ResponseEntity<GenericApiResponse<Void>> response = adminController
                .addUserRolesClient(userId, roles);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("The client roles have been successfully assigned to the user.");
        verify(keycloakService, times(1))
                .addUserRolesClient(userId, roles);
    }

    @Test
    @DisplayName("addUserRolesClient: devrait échouer car userId est vide")
    void addUserRolesClient_ShouldReturnBadRequest_WhenUserIdIsEmpty() {
        // Arrange
        String userId = ""; // UserId vide
        List<RoleRepresentation> roles = Arrays.asList(new RoleRepresentation());

        // Act
        ResponseEntity<GenericApiResponse<Void>> response =
                adminController.addUserRolesClient(userId, roles);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("The user ID is missing");

        // Le service n'est pas invoqué car la validation a échoué
        verifyNoInteractions(keycloakService);
    }

    @Test
    @DisplayName("addUserRolesClient: devrait échouer car la liste des rôles est vide")
    void addUserRolesClient_ShouldReturnBadRequest_WhenRolesListIsEmpty() {
        // Arrange
        String userId = "user123";
        List<RoleRepresentation> roles = Collections.emptyList(); // Liste vide

        // Act
        ResponseEntity<GenericApiResponse<Void>> response =
                adminController.addUserRolesClient(userId, roles);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("The list of roles cannot be empty.");

        // Le service n'est pas invoqué car la validation a échoué
        verifyNoInteractions(keycloakService);
    }

    @Test
    @DisplayName("deleteRoleRealm: devrait valider la suppression des roles assigner " +
            "l'utilisateur du domain")
    public void testDeleteRoleRealm_validInput() {
        // Arrange
        String userId = "user123";
        List<RoleRepresentation> roles = Arrays.asList(new RoleRepresentation());

        // Act
        ResponseEntity<GenericApiResponse<Void>> response = adminController
                .deleteRoleRealm(userId, roles);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNotNull();
        verify(keycloakService, times(1))
                .removeRealmRoleFromUser(userId, roles);
    }

    @Test
    @DisplayName("deleteRoleRealm: devrait échouer car userId est vide")
    void deleteRoleRealm_ShouldReturnBadRequest_WhenUserIdIsEmpty() {
        // Arrange
        String userId = "";
        List<RoleRepresentation> roles = Arrays.asList(new RoleRepresentation());

        // Act
        ResponseEntity<GenericApiResponse<Void>> response =
                adminController.deleteRoleRealm(userId, roles);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("The user ID is missing");

        verifyNoInteractions(keycloakService);
    }

    // Cas d'échec si la liste des rôles est vide
    @Test
    @DisplayName("deleteRoleRealm: devrait échouer car la liste des rôles est vide")
    void deleteRoleRealm_ShouldReturnBadRequest_WhenRolesListIsEmpty() {
        // Arrange
        String userId = "user123";
        List<RoleRepresentation> roles = Collections.emptyList();

        // Act
        ResponseEntity<GenericApiResponse<Void>> response =
                adminController.deleteRoleRealm(userId, roles);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("The list of roles cannot be empty.");

        verifyNoInteractions(keycloakService);
    }

    @Test
    @DisplayName("deleteRoleClient: devrait valider la suppression des roles assigner " +
            "l'utilisateur du sous domain client")
    public void testDeleteClientRole_validInput() {
        // Arrange
        String userId = "user123";
        List<RoleRepresentation> roles = Arrays.asList(new RoleRepresentation());

        // Act
        ResponseEntity<GenericApiResponse<Void>> response = adminController
                .deleteRoleClient(userId, roles);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNotNull();
        verify(keycloakService, times(1))
                .removeClientRoleFromUser(userId, roles);
    }

    @Test
    @DisplayName("deleteRoleClient: devrait échouer car userId est vide")
    void deleteRoleClient_ShouldReturnBadRequest_WhenUserIdIsEmpty() {
        // Arrange
        String userId = "";
        List<RoleRepresentation> roles = Arrays.asList(new RoleRepresentation());

        // Act
        ResponseEntity<GenericApiResponse<Void>> response =
                adminController.deleteRoleClient(userId, roles);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("The user ID is missing");

        verifyNoInteractions(keycloakService);
    }

    // Cas d'échec si la liste des rôles est vide
    @Test
    @DisplayName("deleteRoleClient: devrait échouer car la liste des rôles est vide")
    void deleteRoleClient_ShouldReturnBadRequest_WhenRolesListIsEmpty() {
        // Arrange
        String userId = "user123";
        List<RoleRepresentation> roles = Collections.emptyList();

        // Act
        ResponseEntity<GenericApiResponse<Void>> response =
                adminController.deleteRoleClient(userId, roles);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("The list of roles cannot be empty.");

        verifyNoInteractions(keycloakService);
    }
}