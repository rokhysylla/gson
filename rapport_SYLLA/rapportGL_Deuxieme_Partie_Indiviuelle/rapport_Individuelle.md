
# SYLLA ROKHAYA
## Introduction
Dans la première partie du projet, nous avons réalisé une analyse du code du projet Gson à l’aide de différents outils tels que SonarQube. Cette analyse nous a permis d’identifier plusieurs points d’amélioration, notamment en termes de lisibilité, de structure du code, de complexité des méthodes et de couverture des tests.
L’objectif de cette seconde partie est de proposer et de mettre en œuvre des améliorations concrètes sur le projet.J'ai choisi de me concentrer principalement sur quelques classes du module gson.
Les modifications proposées couvrent différents niveaux de complexité :

- des améliorations simples (suppression de nombres magiques, renommage, nettoyage du code),
- des améliorations intermédiaires (réduction de la complexité, amélioration de la lisibilité),
- ainsi que des améliorations liées aux tests.
Pour chaque modification, nous présenterons :

la situation initiale,
le problème identifié,
la solution proposée,
et l’impact de cette amélioration.

Enfin, une attention particulière sera portée sur ce qu'on a appris en termes de qualité logicielle et de
développement, ainsi qu’à la validation via les tests et les outils d’analyse.

## Petite modification dans JsonReader.

## Moyenne modification dans JsonReader
https://github.com/rokhysylla/gson/blob/main/gson/src/main/java/com/google/gson/stream/JsonReader.java

Plusieurs méthodes de JsonReader, comme nextInt(), nextLong(), nextDouble(), nextXXX et peek(), commençaient par la même logique: vérifier si peeked vaut PEEKED_NONE, puis appeler doPeek() si nécessaire.
Problème
Cette duplication entraîne :
une lisibilité réduite,
une maintenance plus difficile,
un risque d’incohérence si une modification est faite dans une méthode mais pas dans les autres.
Modification

J'ai factorisé la logique commune en introduisant une methodes utilitaires **peekIfNecessary()** et en l'appelant dans chacune des méthodes ou figuraiit la duplication
On a une meilleure lisibilité, réduction de la duplication, centralisation d’une logique commune simple.

`Factorisation de la méthode doPeek()`
La méthode doPeek() était hyper longue et concentrait plusieurs responsabilités :traitement spécifique des objets et tableaux, validation syntaxique et lecture du prochain token.

Cette méthode présentait une complexité importante, ce qui rendait sa lecture et sa maintenance difficiles.J'ai essayé de modifié en partant sur la meme idée mais c'était un peu  risquée à cause de la densité de la logique.

Du coup,J’ai refactorisé doPeek() en extrayant plusieurs méthodes privées :

une méthode pour traiter le scope courant ;
une méthode dédiée au cas des objets ;
une méthode chargée de lire la prochaine valeur.

Maintenant on a :
une réduction de la complexité de la méthode principale ;
une meilleure séparation des responsabilités ;
un code  plus lisible ;

## Petite modification sur LinkedTreeMap

` Renommage des variable e et mine `
Dans la classe LinkedTreeMap, certaines variables locales possèdent des noms peu explicites, comme mine ou e, ce qui rend la lecture du code plus difficile.
Ces noms ne permettent pas de comprendre immédiatement le rôle des variables, ce qui complique la compréhension d’une classe déjà technique.
Renommage de :
mine -> matchingNode
e -> nextNode
On a une compréhension plus rapide du code maintenant.

`Regroupment de certaines méthodes`
J'ai réorganiser certaines classes internes les uns pret des autres vu qu'elles participent à la même fonctionnalité (EntrySet et KeySet).

## Moyenne modification dans LikedTreeMap
https://github.com/rokhysylla/gson/commit/e21a8a1379a5dbe8a150de953fa233bb69bf7e1e

La méthode rebalance() concentrait l’ensemble de la logique de rééquilibrage AVL, avec plusieurs branches conditionnelles et des blocs symétriques pour les cas de déséquilibre à gauche et à droite.

Cette structure rendait la méthode difficile à lire et à maintenir. La présence de calculs répétés de hauteur et de deux branches très similaires augmentait la complexité de la méthode.

J'ai refactorisé la méthode en extrayant :

une méthode utilitaire pour récupérer la hauteur d’un nœud ;
une méthode dédiée au cas de déséquilibre à droite ;
une méthode dédiée au cas de déséquilibre à gauche.
Bénéfices
On a maintenant une
réduction de la complexité de la méthode principale 
meilleure séparation des cas de rééquilibrage 
suppression d’une partie de la duplication 

## Grande modification dans JsonReader

## Moyenne modification dans JsonElement
Plusieurs méthodes de conversion de JsonElement, comme getAsString(), getAsInt() ou getAsBoolean(), lançaient chacune une exception UnsupportedOperationException construite de manière identique.

Cette répétition introduisait une duplication inutile et alourdissait la classe.

Une méthode privée a été  ajoutée **unsupportedConversion()** pour centraliser la création de cette exception, puis les méthodes concernées ont été simplifiées en l’utilisant.
On a maintenant code plus lisible et une suppression de duplication

## Grande modification dans JsonArray et JsonObject
https://github.com/rokhysylla/gson/commit/228c673cee4c86407d930d7b6f546e0685463b93
https://github.com/rokhysylla/gson/commit/228c673cee4c86407d930d7b6f546e0685463b93#diff-5cb391df45caeea8034daf10cb6c490e907a94fd8c690d8961c2144dffa110c7
Les classes JsonObject et JsonArray contenaient plusieurs méthodes répétant la même logique de conversion des valeurs Java vers JsonElement, notamment pour gérer les valeurs null et la création de JsonPrimitive.

Cette duplication alourdissait le code et augmentait le risque d’incohérence entre les différentes méthodes.

Une classe utilitaire JsonElementConversion(lien:https://github.com/rokhysylla/gson/blob/main/gson/src/main/java/com/google/gson/JsonElementConversion.java) a été introduite pour centraliser :

la conversion de String, Number, Boolean et Character vers JsonElement,
la normalisation des références JsonElement nulles vers JsonNull.INSTANCE.

Les méthodes de JsonObject et JsonArray ont ensuite été simplifiées pour utiliser cette nouvelle classe.

On a maintenant une :
suppression de duplication dans plusieurs classes ;
centralisation d’une responsabilité commune ;

## Petite modification dans TypeAdapteur
Dans le début de la classe on voit un bloc de commentaire non javaDoc dont j'ai du suprrimer pour une meilleure lisibilité et aussi diminuer le nombre totale de ligne de commentaire sur cette classe.
Voici le lien: https://github.com/rokhysylla/gson/commit/ca586ced1ad6e5d26583d2c44b0d944f3d1b722a

## Grande modification dans TypeAdapteur
 
La classe TypeAdapter regroupait plusieurs responsabilités. Elle définissait à la fois le contrat principal d’adaptation (read et write), le mécanisme de décoration avec nullSafe(), ainsi que plusieurs méthodes utilitaires de conversion vers et depuis différents supports comme String, Reader, Writer et JsonElement.

Cette concentration de responsabilités rendait la classe plus dense et moins claire. La responsabilité principale d’un TypeAdapter est de définir comment un type est lu depuis JSON et écrit vers JSON. 

Maintenant ,j'ai rajouté une nouvelle classe TypeAdapterSupport(lien:https://github.com/rokhysylla/gson/commit/20aef486a1c501efadd850e4e7aaa9335a373755) pour centraliser les conversions utilitaires :
toJson(Writer, T)
toJson(T)
toJsonTree(T)
fromJson(Reader)
fromJson(String)
fromJsonTree(JsonElement)

Les méthodes publiques correspondantes ont été conservées dans TypeAdapter pour préserver l’API existante, mais elles délèguent désormais leur logique à cette nouvelle classe(lien: https://github.com/rokhysylla/gson/commit/3d48f57d90eb613f23b8ed2e6bcd0686826586fc).
Maintenant On a :
séparation plus claire des responsabilités ;
classe TypeAdapter allégée ;
meilleure lisibilité ;
architecture plus modulaire ;
maintenance facilitée.
