/////////////////////////////////////////////////////////////////////////
//
// © University of Southampton IT Innovation Centre, 2016
//
// Copyright in this software belongs to University of Southampton
// IT Innovation Centre of Gamma House, Enterprise Road,
// Chilworth Science Park, Southampton, SO16 7NS, UK.
//
// This software may not be used, sold, licensed, transferred, copied
// or reproduced in whole or in part in any manner or form or in or
// on any media by any person other than in accordance with the terms
// of the Licence Agreement supplied with the software, or otherwise
// without the prior written consent of the copyright owners.
//
// This software is distributed WITHOUT ANY WARRANTY, without even the
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
// PURPOSE, except where stated in the Licence Agreement supplied with
// the software.
//
//      Created By :          Gianluca Correndo
//      Created Date :        27 Jul 2016
//      Modified by:          Ken Meacham
//      Created for Project : 5G-Ensure
//
/////////////////////////////////////////////////////////////////////////
package uk.ac.soton.itinnovation.security.systemmodeller.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import uk.ac.soton.itinnovation.security.systemmodeller.model.ModelACL;
import java.util.ArrayList;

public interface IModelRepository extends MongoRepository<ModelACL, String> {

	public ModelACL findOneById(String id);

    @Query("{'ownerUrl.url': ?0}")
	public ModelACL findOneByOwnerUrl(String ownerId);

    @Query("{'writeUrl.url': ?0}")
	public ModelACL findOneByWriteUrl(String writeId);

    @Query("{'readUrl.url': ?0}")
	public ModelACL findOneByReadUrl(String readId);

    @Query("{'noRoleUrl.url': ?0}")
	public ModelACL findOneByNoRoleUrl(String noRoleId);

	public ArrayList<ModelACL> findByUserId(String userId);

	public ArrayList<ModelACL> findByOwnerUsernamesContains(String username);
	public ArrayList<ModelACL> findByWriteUsernamesContains(String username);
	public ArrayList<ModelACL> findByReadUsernamesContains(String username);

	public ModelACL findOneByUserIdAndId(String userId, String id);
}
