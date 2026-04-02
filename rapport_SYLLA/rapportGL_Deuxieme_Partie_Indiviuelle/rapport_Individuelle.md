
# SYLLA ROKHAYA
## Introduction
Dans la première partie du projet, nous avons réalisé une analyse du code du projet Gson à l’aide de différents outils tels que SonarQube. Cette analyse nous a permis d’identifier plusieurs points d’amélioration, notamment en termes de lisibilité, de structure du code, de complexité des méthodes et de couverture des tests.
L’objectif de cette seconde partie est de proposer et de mettre en œuvre des améliorations concrètes sur le projet.J'ai choisi de me concentrer principalement sur quelques classes du module gson, notamment JsonReader, JsonWriter et JsonParser.
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

## Petite modification dans JsonReader
Plusieurs méthodes de JsonReader, comme nextInt, nextLong, nextDouble, nextXXX et peek, commençaient par la même logique: vérifier si peeked vaut PEEKED_NONE, puis appeler doPeek() si nécessaire.
Problème
Cette duplication entraîne :
une lisibilité réduite,
une maintenance plus difficile,
un risque d’incohérence si une modification est faite dans une méthode mais pas dans les autres.
Modification

J'ai factorisé la logique commune en introduisant une methodes utilitaires **peekIfNecessary()**
On a une meilleure lisibilité, réduction de la duplication, centralisation d’une logique commune simple.