import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import models.{Category, CategoryJson}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, MustMatchers}
import repository.CategoryRepository
import services.{ConfigService, FlywayService, PostgresService}
import controllers.CategoryController
import helpers.CategorySpecHelper


class CategoryEndPointSpec extends AsyncWordSpec
  with MustMatchers
  with BeforeAndAfterAll
  with ConfigService
  with WebApi
  with ScalatestRouteTest
  with CategoryJson {

  override implicit val executor = system.dispatcher

  val flywayService = new FlywayService(jdbcUrl, dbUser, dbPassword)

  val databaseService = new PostgresService(jdbcUrl, dbUser, dbPassword)

  val categoryRepository = new CategoryRepository(databaseService)

  val categorySpecHelper = new CategorySpecHelper(categoryRepository)

  val categoryController = new CategoryController(categoryRepository)

  override def beforeAll {
    flywayService.migrateDatabase
  }

  override def afterAll {
    flywayService.dropDatabase
  }

  "A category endpoint" must {

    "return an empty list at the beginning" in {
      Get("/categories") ~> categoryController.routes ~> check {
        status mustBe StatusCodes.OK
      }
    }

    "return all the categories when there is atleast one" in {
      categorySpecHelper.createAndDelete() { c =>
        Get("/categories") ~> categoryController.routes ~> check {
          status mustBe StatusCodes.OK
          val categories = responseAs[List[Category]]

          categories must have size 1
        }
      }
    }

    "return BadRequest with repeated titles" in {
      categorySpecHelper.createAndDelete() { c =>
        Post("/categories/", categorySpecHelper.category) ~> categoryController.routes ~> check {
          status mustBe StatusCodes.BadRequest
        }
      }
    }

    "create a category" in {
      Post("/categories/", categorySpecHelper.category) ~> categoryController.routes ~> check {
        status mustBe StatusCodes.Created

        val category = responseAs[Category]

        categoryRepository.delete(category.id.get)

        category.id mustBe defined

        category.title mustBe categorySpecHelper.category.title
      }
    }

    "return NotFound when we delete a non existend category" in {
      Delete("/categories/22/") ~> categoryController.routes ~> check {
        status mustBe StatusCodes.NotFound
      }
    }

    "return no content when we delete a category" in {
      categoryRepository.create(categorySpecHelper.category) flatMap { c =>

        Delete(s"/categories/${c.id.get}/") ~> categoryController.routes ~> check {
          status mustBe StatusCodes.NoContent
        }
      }
    }

  }
}
