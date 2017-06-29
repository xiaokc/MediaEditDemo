#version 120

precision mediump float;

uniform sampler2D u_TextureUnit; // 实际的纹理数据
varying vec2 v_TextureCoordiantes; // 传过来的纹理坐标

void main() {
    gl_FragColor=texture2D(u_TextureUnit,v_TextureCoordiantes); // 获取纹理对象指定坐标位置的颜色，这个颜色即是此片段最终颜色
}
