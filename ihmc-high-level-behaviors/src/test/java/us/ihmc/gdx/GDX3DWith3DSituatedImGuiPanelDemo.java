package us.ihmc.gdx;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiMouseButton;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL41;
import org.lwjgl.system.MemoryUtil;
import us.ihmc.gdx.imgui.ImGuiTools;
import us.ihmc.gdx.sceneManager.GDX3DSceneManager;
import us.ihmc.gdx.sceneManager.GDX3DSceneTools;
import us.ihmc.gdx.tools.GDXApplicationCreator;
import us.ihmc.gdx.tools.GDXModelPrimitives;

import static com.badlogic.gdx.graphics.VertexAttributes.Usage.*;
import static com.badlogic.gdx.graphics.VertexAttributes.Usage.TextureCoordinates;
import static org.lwjgl.glfw.GLFW.*;

public class GDX3DWith3DSituatedImGuiPanelDemo
{
   private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
   private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
   private long windowHandle;

   public GDX3DWith3DSituatedImGuiPanelDemo()
   {
      GDX3DSceneManager sceneManager = new GDX3DSceneManager();
      GDXApplicationCreator.launchGDXApplication(new Lwjgl3ApplicationAdapter()
      {
         private int width = 400;
         private int height = 500;
         private int mouseX = 0;
         private int mouseY = 0;
         private boolean mouseDown = false;
         private FrameBuffer frameBuffer;

         @Override
         public void create()
         {
            sceneManager.create();

            sceneManager.addModelInstance(new ModelInstance(GDXModelPrimitives.createCoordinateFrame(0.3)));

            GLFWErrorCallback.createPrint(System.err).set();

            if (!glfwInit())
            {
               throw new IllegalStateException("Unable to initialize GLFW");
            }

            ImGui.createContext();

            final ImGuiIO io = ImGui.getIO();
            io.setIniFilename(null); // We don't want to save .ini file
            io.setMouseDrawCursor(true);
            ImGuiTools.setupFonts(io);
//            ImGui.styleColorsLight();
//            io.setDisplaySize(width, height);

//            windowHandle = ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle();

            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            windowHandle = GLFW.glfwCreateWindow(width, height, "", MemoryUtil.NULL, MemoryUtil.NULL);

            imGuiGlfw.init(windowHandle, true);
            String glslVersion = null;
            imGuiGl3.init(glslVersion);

            ModelBuilder modelBuilder = new ModelBuilder();
            modelBuilder.begin();

            MeshBuilder meshBuilder = new MeshBuilder();
            meshBuilder.begin(Position | Normal | ColorUnpacked | TextureCoordinates, GL41.GL_TRIANGLES);

            // Counter clockwise order
            // Draw so thumb faces away and index right
            Vector3 topLeftPosition = new Vector3(0.0f, 0.0f, 0.0f);
            Vector3 bottomLeftPosition = new Vector3(0.0f, (float) height, 0.0f);
            Vector3 bottomRightPosition = new Vector3((float) width, (float) height, 0.0f);
            Vector3 topRightPosition = new Vector3((float) width, 0.0f, 0.0f);
            Vector3 topLeftNormal = new Vector3(0.0f, 0.0f, 1.0f);
            Vector3 bottomLeftNormal = new Vector3(0.0f, 0.0f, 1.0f);
            Vector3 bottomRightNormal = new Vector3(0.0f, 0.0f, 1.0f);
            Vector3 topRightNormal = new Vector3(0.0f, 0.0f, 1.0f);
            Vector2 topLeftUV = new Vector2(0.0f, 1.0f);
            Vector2 bottomLeftUV = new Vector2(0.0f, 0.0f);
            Vector2 bottomRightUV = new Vector2(1.0f, 0.0f);
            Vector2 topRightUV = new Vector2(1.0f, 1.0f);
            meshBuilder.vertex(topLeftPosition, topLeftNormal, Color.WHITE, topLeftUV);
            meshBuilder.vertex(bottomLeftPosition, bottomLeftNormal, Color.WHITE, bottomLeftUV);
            meshBuilder.vertex(bottomRightPosition, bottomRightNormal, Color.WHITE, bottomRightUV);
            meshBuilder.vertex(topRightPosition, topRightNormal, Color.WHITE, topRightUV);
            meshBuilder.triangle((short) 3, (short) 0, (short) 1);
            meshBuilder.triangle((short) 1, (short) 2, (short) 3);

            Mesh mesh = meshBuilder.end();

            MeshPart meshPart = new MeshPart("xyz", mesh, 0, mesh.getNumIndices(), GL41.GL_TRIANGLES);
            Material material = new Material();

//            TextureProvider.FileTextureProvider fileTextureProvider = new TextureProvider.FileTextureProvider();
//            Texture debugImageTexture = fileTextureProvider.load("debugImageTexture.jpg");
//            material.set(TextureAttribute.createDiffuse(debugImageTexture));

            GLFrameBuffer.FrameBufferBuilder frameBufferBuilder = new GLFrameBuffer.FrameBufferBuilder(width, height);
            frameBufferBuilder.addBasicColorTextureAttachment(Pixmap.Format.RGBA8888);
            frameBuffer = frameBufferBuilder.build();
            Texture colorBufferTexture = frameBuffer.getColorBufferTexture();
            material.set(TextureAttribute.createDiffuse(colorBufferTexture));

            material.set(ColorAttribute.createDiffuse(Color.WHITE));
            modelBuilder.part(meshPart, material);

            Model model = modelBuilder.end();
            ModelInstance modelInstance = new ModelInstance(model);
            modelInstance.transform.scale(0.005f, 0.005f, 0.005f);
            sceneManager.addRenderableProvider(modelInstance);

            sceneManager.addLibGDXInputProcessor(new InputAdapter()
            {
               @Override
               public boolean mouseMoved(int screenX, int screenY)
               {
                  mouseX = screenX;
                  mouseY = screenY;
                  return false;
               }

               @Override
               public boolean touchDown(int screenX, int screenY, int pointer, int button)
               {
                  mouseDown = true;
                  return false;
               }

               @Override
               public boolean touchUp(int screenX, int screenY, int pointer, int button)
               {
                  mouseDown = false;
                  return false;
               }
            });
         }

         @Override
         public void render()
         {
            GDX3DSceneTools.glClearGray();

            sceneManager.render();


            imGuiGlfw.newFrame();
            ImGuiIO io = ImGui.getIO();
            io.setMousePos(mouseX, mouseY);
            io.setMouseDown(ImGuiMouseButton.Left, mouseDown);
            ImGui.newFrame();


            ImGui.setNextWindowPos(0.0f, 0.0f);
            ImGui.setNextWindowSize(width, height);
            ImGui.begin("Window");
            ImGui.button("I'm a Button!");

            float[] values = new float[100];
            for (int i = 0; i < 100; i++)
            {
               values[i] = i;
            }

            ImGui.plotLines("Histogram", values, 100);

            ImGui.end();

            ImGui.render();
//            imGuiGl3.renderDrawData(ImGui.getDrawData());

            frameBuffer.begin();
            GDX3DSceneTools.glClearGray();
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            frameBuffer.end();

            glfwSwapBuffers(windowHandle);
            glfwPollEvents();
         }

         @Override
         public void dispose()
         {
            sceneManager.dispose();
            imGuiGl3.dispose();
            imGuiGlfw.dispose();

            ImGui.destroyContext();
         }
      }, getClass().getSimpleName(), 1100, 800);
   }

   public static void main(String[] args)
   {
      new GDX3DWith3DSituatedImGuiPanelDemo();
   }
}
