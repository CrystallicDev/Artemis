package fr.natsu.artemis.render;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import fr.natsu.artemis.Artemis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/**
 * GLSL program applied to the silhouette framebuffer to extract an outline (Sobel + FXAA).
 *
 * <p>Adapted from Eterion's shader. The sources are loaded from
 * {@code assets/artemis/shaders/program/}.</p>
 */
public final class OutlineShader {

    private final int programId;
    private final int vertexShaderId;
    private final int fragmentShaderId;

    public OutlineShader(String vertexPath, String fragmentPath) throws IOException {
        String vertexCode = readShaderFromAssets(vertexPath);
        String fragmentCode = readShaderFromAssets(fragmentPath);

        this.vertexShaderId = compileShader(vertexCode, GL20.GL_VERTEX_SHADER);
        this.fragmentShaderId = compileShader(fragmentCode, GL20.GL_FRAGMENT_SHADER);

        this.programId = GL20.glCreateProgram();
        GL20.glAttachShader(this.programId, this.vertexShaderId);
        GL20.glAttachShader(this.programId, this.fragmentShaderId);
        GL20.glLinkProgram(this.programId);

        if (GL20.glGetProgrami(this.programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new IOException("Shader program link failed: "
                + GL20.glGetProgramInfoLog(this.programId, 1024));
        }
    }

    private static String readShaderFromAssets(String path) throws IOException {
        ResourceLocation res = new ResourceLocation(Artemis.MOD_ID, path);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Minecraft.getMinecraft().getResourceManager().getResource(res).getInputStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static int compileShader(String code, int type) throws IOException {
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, code);
        GL20.glCompileShader(shaderId);

        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new IOException("Shader compilation failed: "
                + GL20.glGetShaderInfoLog(shaderId, 1024));
        }
        return shaderId;
    }

    public void bind() {
        GL20.glUseProgram(this.programId);
    }

    public void unbind() {
        GL20.glUseProgram(0);
    }

    public int getUniform(String name) {
        return GL20.glGetUniformLocation(this.programId, name);
    }

    public void cleanup() {
        unbind();
        GL20.glDetachShader(this.programId, this.vertexShaderId);
        GL20.glDetachShader(this.programId, this.fragmentShaderId);
        GL20.glDeleteShader(this.vertexShaderId);
        GL20.glDeleteShader(this.fragmentShaderId);
        GL20.glDeleteProgram(this.programId);
    }
}
