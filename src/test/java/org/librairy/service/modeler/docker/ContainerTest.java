package org.librairy.service.modeler.docker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ProgressMessage;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class ContainerTest {


    private static final Logger LOG = LoggerFactory.getLogger(ContainerTest.class);

    @Test
    public void createImage() throws DockerCertificateException, InterruptedException, DockerException, IOException {


        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.init();

        Template t = velocityEngine.getTemplate("src/test/docker/Dockerfile.vm");

        VelocityContext context = new VelocityContext();
        context.put("title", "Hi Crazy World!");


        FileWriter fw = new FileWriter("src/test/docker/Dockerfile");
        t.merge( context, fw);
        fw.close();



        // Create a client based on DOCKER_HOST and DOCKER_CERT_PATH env vars
        final DockerClient docker = new DefaultDockerClient("unix:///var/run/docker.sock");


        List<Container> containers = docker.listContainers();


        final AtomicReference<String> imageIdFromMessage = new AtomicReference<>();

        final String returnedImageId = docker.build(
                Paths.get("src/test/docker"), "librairy/java-model:latest", new ProgressHandler() {
                    @Override
                    public void progress(ProgressMessage message) throws DockerException {
                        final String imageId = message.buildImageId();
                        if (imageId != null) {
                            imageIdFromMessage.set(imageId);
                        }
                    }
                });


        LOG.info("Image built: " + imageIdFromMessage);

        LOG.info("Image Id: " + returnedImageId);

    }
}
