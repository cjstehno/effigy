/*
 * Copyright (c) 2015 Christopher J. Stehno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package people.repository

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import people.DatabaseEnvironment
import people.entity.Image

import static org.springframework.test.jdbc.JdbcTestUtils.countRowsInTable

class ImageRepositoryTest {

    @Rule public DatabaseEnvironment database = new DatabaseEnvironment()

    private EffigyImageRepository imageRepository

    @Before void before() {
        imageRepository = new EffigyImageRepository(jdbcTemplate: database.jdbcTemplate)
    }

    @Test void 'create: one'() {
        def imageId = imageRepository.create(new Image(description: 'Some photo', contentLength: 100))

        assert imageId
        assert countRowsInTable(database.jdbcTemplate, 'images') == 1
    }

    @Test void 'retrieve: one'() {
        def (idA, idB, idC) = createThree()

        assert imageRepository.retrieve(idA).description == 'Photo-A'
        assert imageRepository.retrieve(idB).description == 'Photo-B'
        assert imageRepository.retrieve(idC).description == 'Photo-C'
    }

    @Test void 'retrieve: all'() {
        def (idA, idB, idC) = createThree()

        def images = imageRepository.retrieveAll()

        assert images.size() == 3
    }

    @Test void 'update'() {
        def orig = imageRepository.retrieve(imageRepository.create(new Image(description: 'Some photo', contentLength: 200)))

        orig.description = 'Updated'

        assert imageRepository.update(orig)

        assert countRowsInTable(database.jdbcTemplate, 'images') == 1
        assert imageRepository.retrieve(orig.id).description == 'Updated'
    }

    private createThree() {
        def a = imageRepository.create(new Image(description: 'Photo-A', contentLength: 100))
        def b = imageRepository.create(new Image(description: 'Photo-B', contentLength: 110))
        def c = imageRepository.create(new Image(description: 'Photo-C', contentLength: 120))
        [a, b, c]
    }
}
