package com.altankoc.nidado.data.repository

import com.altankoc.nidado.data.dao.CourseDao
import com.altankoc.nidado.data.mapper.toCourse
import com.altankoc.nidado.data.mapper.toCourseEntity
import com.altankoc.nidado.domain.model.Course
import com.altankoc.nidado.domain.repository.CourseRepository
import com.altankoc.nidado.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CourseRepositoryImpl @Inject constructor(
    private val courseDao: CourseDao
) : CourseRepository {

    override fun getAllCoursesDesc(): Flow<List<Course>> {
        return courseDao.getAllCoursesDesc().map { list ->
            list.map { it.toCourse() }
        }
    }

    override fun getAllCoursesAsc(): Flow<List<Course>> {
        return courseDao.getAllCoursesAsc().map { list ->
            list.map { it.toCourse() }
        }
    }

    override suspend fun getCourseById(id: Int): Course? {
        return courseDao.getCourseById(id)?.toCourse()
    }

    override suspend fun insertCourse(course: Course): Resource<Unit> {
        return try {
            courseDao.insertCourse(course.toCourseEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Ders eklenirken hata oluştu")
        }
    }

    override suspend fun updateCourse(course: Course): Resource<Unit> {
        return try {
            courseDao.updateCourse(course.toCourseEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Ders güncellenirken hata oluştu")
        }
    }

    override suspend fun deleteCourse(course: Course): Resource<Unit> {
        return try {
            courseDao.deleteCourse(course.toCourseEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Ders silinirken hata oluştu")
        }
    }
}