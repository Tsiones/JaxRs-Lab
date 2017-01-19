package se.github.jaxrs.service;

import static se.github.jaxrs.loader.ContextLoader.getBean;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.activation.UnsupportedDataTypeException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import se.github.jaxrs.jsonupdater.JsonFieldUpdater;
import se.github.logger.MultiLogger;
import se.github.springlab.model.User;
import se.github.springlab.repository.UserRepository;
import se.github.springlab.service.TaskerService;

/**
 * TODO: cleanup userservice query methods
 * 
 *
 */
@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserService extends AbstractService
{
	private static TaskerService service = getBean(TaskerService.class);
	private static UserRepository userRepo = service.getUserRepository();

	static
	{
		MultiLogger.createLogger("UserServiceLog");
	}

	@Context
	UriInfo uriInfo;

	@POST
	public Response create(User user)
	{
		User newUser = service.update(user);
		URI location = uriInfo.getAbsolutePathBuilder().path(getClass(), "getOne").build(user.getId());
		MultiLogger.log("UserServiceLog", Level.INFO, "Created user: " + user.toString());
		return Response.ok(newUser).contentLocation(location).build();
	}

	@GET
	public Response get()
	{
		if (uriInfo.getQueryParameters().isEmpty())
		{
			Collection<User> result = new HashSet<>();
			userRepo.findAll().forEach(e -> result.add(e));
			GenericEntity<Collection<User>> entity = new GenericEntity<Collection<User>>(result)
			{
			};

			return Response.ok(entity).build();
		}
		for (Entry<String, List<String>> entry : uriInfo.getQueryParameters().entrySet())
		{
			String key = entry.getKey();
			String searchToken = uriInfo.getQueryParameters().getFirst(key);
			switch (key.toLowerCase())
			{
			case "getby":
				return getByQuery(searchToken);
			case "searchby":
				return searchByQuery(searchToken);
			}
		}
		throw new WebApplicationException(Status.BAD_REQUEST);

	}

	private long getQueryID()
	{
		try
		{
			return Long.parseLong(uriInfo.getQueryParameters().getFirst("q"));
		}
		catch (NumberFormatException e)
		{
			throw new WebApplicationException("Correct query uri: users/?<getby/searchby>=<entity>&q=<id>", Status.BAD_REQUEST);
		}
	}

	//getBy
	private Response getByQuery(String searchToken)
	{
		if (searchToken.equals("team"))
		{
			Collection<User> result = userRepo.findByTeamId(getQueryID());
			if (result.isEmpty())
			{
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			GenericEntity<Collection<User>> entity = new GenericEntity<Collection<User>>(result)
			{
			};

			return Response.ok(entity).build();
		}
		throw new WebApplicationException(Status.BAD_REQUEST);
	}

	//searchBy
	private Response searchByQuery(String searchToken)
	{
		String searchId = String.valueOf(getQueryID());
		Collection<User> result = null;

		switch (searchToken.toLowerCase())
		{
		case "firstname":
		{
			result = userRepo.findByFirstName(searchId);
			break;
		}
		case "lastname":
		{
			result = userRepo.findByLastName(searchId);
			break;
		}
		case "username":
		{
			result = userRepo.findByUsername(searchId);
			break;
		}
		case "usernumber":
		{
			User user = userRepo.findByUserNumber(searchId);
			result = new HashSet<>();
			result.add(user);
			break;
		}

		default:
			throw new WebApplicationException("Unknown search token! Valid tokens: firstname, lastname, username and usernumber", Status.BAD_REQUEST);
		}
		if (result.isEmpty())
		{
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		GenericEntity<Collection<User>> entity = new GenericEntity<Collection<User>>(result)
		{
		};
		return Response.ok(entity).build();
	}

	@GET
	@Path("{id}")
	public User getOne(@PathParam("id") Long id)
	{
		if (userRepo.exists(id))
		{
			return userRepo.findOne(id);
		}
		throw new WebApplicationException(Status.NOT_FOUND);
	}

	@DELETE
	@Path("{id}")
	public Response remove(@PathParam("id") Long id)
	{
		if (userRepo.exists(id))
		{
			userRepo.delete(id);
			return Response.ok().build();
		}
		return Response.status(Status.NOT_FOUND).build();
	}

	@PUT
	@Path("{id}")
	public User update(@PathParam("id") Long id, String json) throws UnsupportedDataTypeException, IllegalArgumentException, IllegalAccessException
	{
		JsonObject jsonObject = (JsonObject) new JsonParser().parse(json);
		User user = userRepo.findOne(id);
		JsonFieldUpdater.modifyWithJson(user, jsonObject);

		return userRepo.save(user);
	}

}
