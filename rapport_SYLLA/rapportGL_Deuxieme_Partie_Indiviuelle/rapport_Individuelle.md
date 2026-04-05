
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
https://github.com/rokhysylla/gson/commit/9d1e0eb702ea88c7396bcd12cfca137800bf6dbe

Dans JsonReader on trouvait des valeurs appelées `nombres magiques` que j’ai remplacées par des constantes explicites.

## 2  Moyenne modification dans JsonReader
https://github.com/rokhysylla/gson/commit/b278b21f4d5d8a7b40dbbc5f3bf4e63977dc0985
1. Refactorisation des methodes nextXXX

Plusieurs méthodes de JsonReader, comme nextInt(), nextLong(), nextDouble(), nextXXX et peek(), commençaient par la même logique: vérifier si peeked vaut PEEKED_NONE, puis appeler doPeek() si nécessaire.
Problème
Cette duplication entraîne :
une lisibilité réduite,
une maintenance plus difficile,
un risque d’incohérence si une modification est faite dans une méthode mais pas dans les autres.
Modification

J'ai factorisé la logique commune en introduisant une methodes utilitaires `peekIfNecessary()` et en l'appelant dans chacune des méthodes ou figuraiit la duplication
On a une meilleure lisibilité, réduction de la duplication, centralisation d’une logique commune simple.

2. Factorisation de la méthode doPeek()  https://github.com/rokhysylla/gson/commit/270244d1dcc48630800e48e3669de6ec1d100470#diff-0ddecddf64de180e8e9bc664a064e57ee4475426b3027089cf940ca48e6f8b19

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

## 2 Petites modifications sur LinkedTreeMap

1. Renommage des variable e et mine https://github.com/rokhysylla/gson/commit/c02435351bc87edde596d03ac66a1630911e63f8#diff-270f63d2d902ec3cb3993f2d937e9dedf91c574e99eec2e076e606e9a4e63441
   Dans la classe LinkedTreeMap, certaines variables locales possèdent des noms peu explicites, comme mine ou e, ce qui rend la lecture du code plus difficile.
   Ces noms ne permettent pas de comprendre immédiatement le rôle des variables, ce qui complique la compréhension d’une classe déjà technique.
   Renommage de :
   mine -> matchingNode
   e -> nextNode
   On a une compréhension plus rapide du code maintenant.

2. Regroupment de certaines méthodes https://github.com/rokhysylla/gson/commit/774f20497254add74e27ecf375b8315ed094be39

   J'ai réorganiser certaines classes internes les uns pret des autres vu qu'elles participent à la même fonctionnalité (EntrySet et KeySet).

## Moyenne modification dans LinkedTreeMap
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

## Moyenne modification dans JsonElement
https://github.com/rokhysylla/gson/commit/e69de9933ac101885f0062aca3eb2a4af9e82ccb#diff-15ab1794dd57f701d2e2f8ba8b7828ddde078648a0be49b25770c0beefeacaf5

Plusieurs méthodes de conversion de JsonElement, comme getAsString(), getAsInt() ou getAsBoolean(), lançaient chacune une exception UnsupportedOperationException construite de manière identique.

Cette répétition introduisait une duplication inutile et alourdissait la classe.

Une méthode privée a été  ajoutée `unsupportedConversion()` pour centraliser la création de cette exception, puis les méthodes concernées ont été simplifiées en l’utilisant.
On a maintenant code plus lisible et une suppression de duplication

## Grande modification dans JsonArray et JsonObject
(JsonArray : https://github.com/rokhysylla/gson/commit/228c673cee4c86407d930d7b6f546e0685463b93)

(JsonObject : https://github.com/rokhysylla/gson/commit/228c673cee4c86407d930d7b6f546e0685463b93#diff-5cb391df45caeea8034daf10cb6c490e907a94fd8c690d8961c2144dffa110c7 )
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
Maintenant On obtent une  :
- séparation plus claire des responsabilités ;
- classe TypeAdapter allégée ;
- meilleure lisibilité ;
- architecture plus modulaire ;
- maintenance facilitée.

## 2 Petites modification dans UnsafeAllocator
1. Renommage des variables   (lien: https://github.com/rokhysylla/gson/commit/bfb5bf58678f6d979433da83127dcaa48a061601)

La classe UnsafeAllocator contenait plusieurs noms de variables peu explicites, comme c ou f.
Ces éléments n'étaient pas significatif.

Les noms de variables ont été clarifiés :
c a été renommé en targetClass
f a été renommé en field

2. Suppression de commentaire non javaDoc inutile https://github.com/rokhysylla/gson/commit/01904c48bcf980a2546bf16e6ce3993732ffc9b4

J'ai supprimé deux bloc de commentaire non javadoc pour une meilleure visibilité

## Moyenne modification dans TypeAdapterTest

https://github.com/rokhysylla/gson/commit/b508a47f1d9d15cc72cfaad0823ef17d9e038a3e

La classe TypeAdapter possédait déjà quelques tests, mais certains comportements publics n’étaient pas couverts, notamment après la refactorisation de la logique de conversion vers TypeAdapterSupport.

J'ai ajouté des tests ciblés pour sécuriser les méthodes déléguées vers TypeAdapterSupport

Maintenant on a une
meilleure couverture fonctionnelle
validation de la refactorisation

## 2 Petites modifications dans GsonBuilder https://github.com/rokhysylla/gson/commit/dd863b912b679fa0a6bcb42232433dae2c5fcb07

1. Suppression de nombre magique  
   La méthode checkDateFormatStyle(int style) utilisait directement la valeur 3 pour vérifier si un style de date était valide.

Cette valeur n’était pas explicite
Une constante nommée a été introduite pour représenter la valeur maximale autorisée.

On a maintenant ;
signification plus explicit

2. Renommage d'un paramètre

Le paramètre all utilisé dans la méthode addUserDefinedAdapters(...) était trop générique.

Le paramètre a été renommé en allFactories.
Maintenant on a intention plus claire ,une compréhension plus rapide du code.

## Moyenne Modification dans GsonBuilder
https://github.com/rokhysylla/gson/commit/07ea17f9084cf5ad4e7aa6a67aba1eacbb8ab3b6
Les méthodes registerTypeAdapter(...) et registerTypeHierarchyAdapter(...) effectuaient chacune leur propre validation de l’objet typeAdapter, avec une logique presque identique.

Cette duplication rendait le code plus difficile à maintenir.

La validation a été centralisée dans une méthode privée unique, paramétrée selon l’autorisation ou non d’un InstanceCreator.
On obtient  une:
- réduction de duplication ;
- validation plus cohérente ;

## Petite modiffication GsonBuilderTEst
https://github.com/rokhysylla/gson/commit/36dbe3c3a90ae86edce44d62485f31793acb957c
J'ai supprimé certaine commentaire dans les test car le nom de la methode était déjà significative d'ou le commentaire n'a pas de sens

## Moyenne modification dans GsonBuilderTest
https://github.com/rokhysylla/gson/commit/97666c71f1dec33d3466bad6077d19489b9c62a4
Certains tests de GsonBuilderTest utilisaient plusieurs blocs d’assertions très similaires pour vérifier des exceptions, ce qui introduisait une duplication dans la structure des tests.

Cette répétition réduisait la lisibilité et rendait les tests plus verbeux qu’ils ne devaient l’être.
Une méthode a été introduite pour centraliser la vérification des exceptions IllegalArgumentException et de leur message.

Maintenant on a :
- tests plus lisibles ;
- réduction de duplication ;

## Petites modifications dans Gson
https://github.com/rokhysylla/gson/commit/9ef8db3534c2de3cb579ccfc52cbfdedaa9d60c6
Dans la classe Gson, j’ai effectué un nettoyage léger du code.J'ai supprimé une déclaration unused qui n’était pas réellement nécessaire.La suppresion permet d'avoir un code plus propre.
 
## Moyenne modification dans Gson
https://github.com/rokhysylla/gson/commit/f7a1c4e442fceac0534547042619778ad2f36633
Je me suis concentrée sur les méthodes fromJson(...) qui présentaient une structure très similaire.
Cette duplication rendait le code redondant et moins lisible

J’ai donc simplifié cette partie en réorganisant les appels entre les différentes méthodes fromJson(...), afin d’éviter de répéter la même logique.
L’objectif était de rendre le code plus clair et plus cohérent.

## Conclusion
Dans ce projet, je me suis concentrée sur certaines parties du code où il y avait des problèmes de lisibilité et de duplication.
Même si je n’ai pas tout couvert, j’ai pu apporter plusieurs améliorations concrètes, notamment en factorisant certaines logiques et en simplifiant le code.
Ces modifications permettent d’obtenir un code plus clair, plus cohérent et plus facile à maintenir..