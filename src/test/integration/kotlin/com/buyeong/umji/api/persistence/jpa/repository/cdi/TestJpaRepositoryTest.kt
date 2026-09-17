package com.buyeong.umji.api.persistence.jpa.repository.cdi

import com.buyeong.umji.api.persistence.jpa.entity.cdi.TestJpaEntity
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class TestJpaRepositoryTest(
    private val repository: TestJpaRepository,
) : BehaviorSpec({
    Context("TestJpaRepository CRUD operations") {
        Given("a new TestJpaEntity") {
            val jsonData = listOf(
                mapOf("key1" to "value1", "key2" to "value2"),
                mapOf("key3" to "value3", "key4" to "value4"),
            )

            val entity = TestJpaEntity(
                id = 0,
                json = jsonData,
            )

            When("saving the entity") {
                val savedEntity = repository.save(entity)

                Then("it should be saved with generated ID") {
                    savedEntity.id shouldNotBe 0
                    savedEntity.json shouldBe jsonData
                }

                And("it should be retrievable by ID") {
                    val foundEntity = repository.findByIdOrNull(savedEntity.id)
                    foundEntity shouldNotBe null
                    foundEntity?.id shouldBe savedEntity.id
                    foundEntity?.json shouldBe jsonData
                }
            }
        }

        Given("multiple TestJpaEntities") {
            val entities =
                listOf(
                    TestJpaEntity(
                        id = 0,
                        json = listOf(mapOf("test1" to "data1")),
                    ),
                    TestJpaEntity(
                        id = 0,
                        json = listOf(mapOf("test2" to "data2")),
                    ),
                    TestJpaEntity(
                        id = 0,
                        json = listOf(mapOf("test3" to "data3")),
                    ),
                )

            When("saving all entities") {
                val savedEntities = repository.saveAll(entities)

                Then("all entities should be saved with IDs") {
                    savedEntities.size shouldBe 3
                    savedEntities.forEach { entity ->
                        entity.id shouldNotBe 0
                    }
                }
            }
        }

        Given("an existing TestJpaEntity for update") {
            val originalJson = listOf(mapOf("original" to "data"))
            val entity =
                TestJpaEntity(
                    id = 0,
                    json = originalJson,
                )

            When("updating the entity") {
                // 저장
                val savedEntity = repository.save(entity)
                val savedId = savedEntity.id

                // 업데이트를 위해 다시 조회
                val entityToUpdate = repository.findByIdOrNull(savedId)!!
                val updatedJson =
                    listOf(
                        mapOf("updated" to "data"),
                        mapOf("new" to "value"),
                    )
                entityToUpdate.json = updatedJson
                val updatedEntity = repository.save(entityToUpdate)

                Then("the changes should be persisted") {
                    updatedEntity.id shouldBe savedId
                    updatedEntity.json shouldBe updatedJson
                }

                And("retrieving by ID should return updated data") {
                    val foundEntity = repository.findByIdOrNull(savedId)
                    foundEntity?.json shouldBe updatedJson
                }
            }
        }

        Given("an existing TestJpaEntity for deletion") {
            val entity =
                TestJpaEntity(
                    id = 0,
                    json = listOf(mapOf("delete" to "me")),
                )

            When("deleting the entity") {
                val savedEntity = repository.save(entity)
                val savedId = savedEntity.id

                repository.deleteById(savedId)
                repository.flush() // 삭제 즉시 반영

                Then("it should no longer exist") {
                    val foundEntity = repository.findByIdOrNull(savedId)
                    foundEntity shouldBe null
                }

                And("existsById should return false") {
                    repository.existsById(savedId) shouldBe false
                }
            }
        }

        Given("entities for count operation") {
            When("saving entities and counting") {
                // 테스트용 고유한 식별자를 가진 엔티티 생성
                val testIdentifier = "test-${System.currentTimeMillis()}"
                val entities =
                    (1..3).map { index ->
                        TestJpaEntity(
                            id = 0,
                            json = listOf(mapOf("testId" to testIdentifier, "index" to "entity$index")),
                        )
                    }

                val beforeCount = repository.count()
                val savedEntities = repository.saveAll(entities)

                Then("saved entities should have IDs") {
                    savedEntities.size shouldBe 3
                    savedEntities.forEach { it.id shouldNotBe 0 }
                }

                And("count should increase") {
                    val afterCount = repository.count()
                    (afterCount - beforeCount).toInt() shouldBe 3
                }
            }
        }
    }
})