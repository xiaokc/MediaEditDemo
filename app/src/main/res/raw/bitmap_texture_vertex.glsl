#version 120
attribute vec4 a_Position;

attribute vec2 a_TextureCoordinates;
varying vec2 v_TextureCoordinates;
//uniform mat4 u_MVPMatrix;

void main()
{
    gl_Position=a_Position;
    v_TextureCoordinates=a_TextureCoordinates;
}