Тестовое задание:

1. Запустить тесты из класса [ShipTest](src/test/java/io/tarantool/springdata/example/repository/ShipTest.java)
2. Реализовать тест `testSaveAndFind` который должен содержать
   - запись `tuple` в Tarantool с проверкой возвращаемого значения 
   - запрос `select` в Tarantool, для проверки корректности записанной ранее информации
3. Написать дополнительные тесты покрывающие работу Java и Tarantool
4. *Дополнительное задание
   1. Изменить входные данные `tuple` таким образом, чтобы `testEval` выдавал ошибку (нельзя модифицировать модель Ship).
   2. Починить тест из пункта 4.1.
   