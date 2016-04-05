package software.wings.rules;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.beans.ReadPref;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ArtifactServiceImpl;
import software.wings.service.impl.RoleServiceImpl;
import software.wings.service.impl.UserServiceImpl;
import software.wings.service.impl.WorkflowServiceImpl;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WorkflowService;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 4/5/16.
 */
public class WingsRule implements MethodRule {
  private Injector injector;
  private MongoServer mongoServer;
  private Datastore datastore;
  private int port = 0;

  public WingsRule() {}

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return new Statement() {
      public void evaluate() throws Throwable {
        WingsRule.this.before();
        injector.injectMembers(target);
        try {
          statement.evaluate();
        } finally {
          WingsRule.this.after();
        }
      }
    };
  }

  protected void before() throws Throwable {
    mongoServer = new MongoServer(new MemoryBackend());
    mongoServer.bind("localhost", port);
    InetSocketAddress serverAddress = mongoServer.getLocalAddress();
    MongoClient client = new MongoClient(new ServerAddress(serverAddress));

    Morphia morphia = new Morphia();
    datastore = morphia.createDatastore(client, "wings");

    injector = Guice.createInjector(new ValidationModule(), new AbstractModule() {
      @Override
      protected void configure() {
        bind(Datastore.class).annotatedWith(Names.named("primaryDatasource")).toInstance(datastore);
        bind(Datastore.class).annotatedWith(Names.named("secondaryDatasource")).toInstance(datastore);
        bind(new TypeLiteral<Map<ReadPref, Datastore>>() {})
            .annotatedWith(Names.named("datastoreMap"))
            .toInstance(ImmutableMap.<ReadPref, Datastore>of(ReadPref.CRITICAL, datastore, ReadPref.NORMAL, datastore));
        bind(WingsPersistence.class).to(WingsMongoPersistence.class);
        bind(ArtifactService.class).to(ArtifactServiceImpl.class);
        bind(WorkflowService.class).to(WorkflowServiceImpl.class);
        bind(RoleService.class).to(RoleServiceImpl.class);
        bind(UserService.class).to(UserServiceImpl.class);
      }
    });
  }

  public Datastore getDatastore() {
    return datastore;
  }

  protected void after() {
    mongoServer.shutdown();
    injector.getInstance(WingsPersistence.class).close();
  }
}
