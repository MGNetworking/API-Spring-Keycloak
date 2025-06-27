# Script PowerShell pour automatiser le deploiement Kubernetes et l'inspection des ressources

# Forcer l'encodage UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$PSDefaultParameterValues['Out-File:Encoding'] = 'utf8'

# Fonction pour verifier si une commande existe
function Test-CommandExists {
    param($command)
    $exists = $null -ne (Get-Command $command -ErrorAction SilentlyContinue)
    return $exists
}

# Fonction pour attendre que le pod soit pret
function Wait-PodRunning {
    param($podName)
    $timeoutSeconds = 300  # Temps d'attente maximal (5 minutes)
    $intervalSeconds = 10  # Intervalle entre les verifications
    $elapsed = 0

    Write-Host "Attente que le pod $podName soit dans l'etat Running..."
    while ($elapsed -lt $timeoutSeconds) {
        $status = kubectl get pod $podName -o jsonpath='{.status.phase}'
        if ($status -eq "Running") {
            Write-Host "Pod $podName est dans l'etat Running."
            return $true
        }
        Write-Host "Pod $podName est dans l'etat $status. Attente $intervalSeconds secondes..."
        Start-Sleep -Seconds $intervalSeconds
        $elapsed += $intervalSeconds
    }
    Write-Error "Le pod $podName n'est pas passe a l'etat Running apres $timeoutSeconds secondes."
    return $false
}

# Verifier si kubectl est installe
Write-Host "Verification de l'installation de kubectl..."
if (-not (Test-CommandExists kubectl)) {
    Write-Error "kubectl n'est pas installe ou n'est pas dans le PATH. Veuillez installer kubectl."
    exit 1
}

# Verifier si les fichiers YAML existent
$yamlFiles = @("./k8s/deployment.yaml", "./k8s/service.yaml", "./k8s/ingress.yaml")
foreach ($file in $yamlFiles) {
    if (-not (Test-Path $file)) {
        Write-Error "Le fichier $file n'existe pas. Verifiez le chemin."
        exit 1
    }
}

# Etape 1 : Appliquer les fichiers YAML
Write-Host "Application des fichiers YAML Kubernetes..."
foreach ($file in $yamlFiles) {
    Write-Host "Application de $file..."
    try {
        kubectl apply -f $file
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Echec de l'application du fichier $file."
            exit 1
        }
        Write-Host "Fichier $file applique avec succes."
    }
    catch {
        Write-Error "Erreur lors de l'application du fichier $file : $_"
        exit 1
    }
}

# Etape 2 : Recuperer les informations sur l'ingress
Write-Host "Recuperation des informations sur l'ingress..."
try {
    kubectl get ingress
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Echec de la commande kubectl get ingress."
        exit 1
    }
}
catch {
    Write-Error "Erreur lors de la recuperation des ingresses : $_"
    exit 1
}

# Etape 3 : Recuperer les informations sur les pods
Write-Host "Recuperation des informations sur les pods..."
try {
    kubectl get pods
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Echec de la commande kubectl get pods."
        exit 1
    }
}
catch {
    Write-Error "Erreur lors de la recuperation des pods : $_"
    exit 1
}

# Etape 4 : Recuperer le nom du pod
Write-Host "Recuperation du nom du pod..."
try {
    $podName = kubectl get pods -o jsonpath='{.items[0].metadata.name}'
    if (-not $podName) {
        Write-Error "Aucun pod trouve ou erreur lors de la recuperation du nom du pod."
        exit 1
    }
    Write-Host "Pod trouve : $podName"
}
catch {
    Write-Error "Erreur lors de la recuperation du nom du pod : $_"
    exit 1
}

# Etape 5 : Attendre que le pod soit pret
if (-not (Wait-PodRunning -podName $podName)) {
    Write-Error "Le pod $podName n'est pas pret. Arret du script."
    exit 1
}

# Etape 6 : Afficher les details du pod
Write-Host "Affichage des details du pod $podName..."
try {
    kubectl describe pod $podName
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Echec de la commande kubectl describe pod $podName."
        exit 1
    }
}
catch {
    Write-Error "Erreur lors de l'affichage des details du pod : $_"
    exit 1
}

# Etape 7 : Afficher les logs du pod en mode suivi
Write-Host "Affichage des logs du pod $podName (mode suivi)..."
try {
    kubectl logs -f $podName
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Echec de la commande kubectl logs -f $podName."
        exit 1
    }
}
catch {
    Write-Error "Erreur lors de l'affichage des logs du pod : $_"
    exit 1
}

Write-Host "Toutes les operations ont ete completees avec succes!" -ForegroundColor Green