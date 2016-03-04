package com.crossbowffs.waifupaper.rendering

import android.graphics.Bitmap
import android.opengl.GLUtils
import com.crossbowffs.waifupaper.utils.useNotNull
import jp.live2d.android.UtOpenGL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10

// Adapted from:
// http://obviam.net/index.php/texture-mapping-opengl-android-displaying-images-using-opengl-and-squares/
class GLBitmap(private val bitmap: Bitmap) {
    private var gl: GL10? = null
    private var textureId: Int? = null

    val vertices = floatArrayOf(
        -1.0f, -1.0f, // V1 - bottom left
        -1.0f, +1.0f, // V2 - top left
        +1.0f, -1.0f, // V3 - bottom right
        +1.0f, +1.0f  // V4 - top right
    )

    val textureCoords = floatArrayOf(
        0.0f, 1.0f, // top left     (V2)
        0.0f, 0.0f, // bottom left	(V1)
        1.0f, 1.0f, // top right	(V4)
        1.0f, 0.0f  // bottom right	(V3)
    )

    val verticesBuffer: FloatBuffer
    val textureCoordsBuffer: FloatBuffer

    init {
        val vertexByteBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
        vertexByteBuffer.order(ByteOrder.nativeOrder())
        verticesBuffer = vertexByteBuffer.asFloatBuffer()
        verticesBuffer.put(vertices)
        verticesBuffer.position(0)

        val textureByteBuffer = ByteBuffer.allocateDirect(textureCoords.size * 4)
        textureByteBuffer.order(ByteOrder.nativeOrder())
        textureCoordsBuffer = textureByteBuffer.asFloatBuffer()
        textureCoordsBuffer.put(textureCoords)
        textureCoordsBuffer.position(0)
    }

    private fun bitmapToTexture(gl: GL10, bitmap: Bitmap): Int {
        val id = UtOpenGL.genTexture(gl)
        gl.glBindTexture(GL10.GL_TEXTURE_2D, id)
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR.toFloat())
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat())
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE.toFloat())
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE.toFloat())
        gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE.toFloat())
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0)
        return id
    }

    fun setGL(newGl: GL10?) {
        val oldGl = gl
        if (newGl == oldGl) return
        val oldTextureId = textureId
        if (oldTextureId != null && oldGl != null) {
            oldGl.glDeleteTextures(1, intArrayOf(oldTextureId), 0)
        }

        gl = newGl
        textureId = newGl.useNotNull { bitmapToTexture(it, bitmap) }
    }

    fun setBounds(left: Float, right: Float, bottom: Float, top: Float) {
        vertices[0] = left
        vertices[1] = bottom
        vertices[2] = left
        vertices[3] = top
        vertices[4] = right
        vertices[5] = bottom
        vertices[6] = right
        vertices[7] = top
        verticesBuffer.clear()
        verticesBuffer.put(vertices)
        verticesBuffer.position(0)
    }

    fun draw(gl: GL10) {
        assert(gl == this.gl)

        // bind the previously generated texture
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId!!)

        // Point to our buffers
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY)

        // Set the face rotation
        gl.glFrontFace(GL10.GL_CW)

        // Point to our vertex buffer
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, verticesBuffer)
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureCoordsBuffer)

        // Draw the vertices as triangle strip
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.size / 2)

        // Disable the client state before leaving
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
    }
}
