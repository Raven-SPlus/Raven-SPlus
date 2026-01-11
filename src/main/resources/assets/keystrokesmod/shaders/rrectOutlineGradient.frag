#version 120

uniform vec2 location, rectSize;
uniform vec4 color;
uniform vec4 outlineColor1, outlineColor2;
uniform float radius, outlineThickness;

float roundedSDF(vec2 centerPos, vec2 size, float radius) {
    return length(max(abs(centerPos) - size + radius, 0.0)) - radius;
}

vec4 gradientOutlineColor(vec2 fragCoord) {
    float t = clamp((fragCoord.x - location.x) / rectSize.x, 0.0, 1.0);
    return mix(outlineColor1, outlineColor2, t);
}

void main() {
    vec2 frag = gl_FragCoord.xy;
    vec2 center = location + (rectSize * .5);
    vec2 halfSize = (rectSize * .5);
    
    // Distance to the rounded rectangle edge (negative = inside, positive = outside)
    float distance = roundedSDF(frag - center, halfSize, radius);
    
    // Anti-aliasing: smooth transition over ~2 pixels
    float aaRange = 2.0;
    
    // Fill alpha: smooth fade at edges (inside the shape)
    float fillAlpha = 1.0 - smoothstep(-aaRange, 0.0, distance);
    
    // Outline alpha: smooth fade from edge outward
    float outlineDist = abs(distance);
    float outlineAlpha = 1.0 - smoothstep(0.0, outlineThickness + aaRange, outlineDist);
    
    vec4 outCol = gradientOutlineColor(frag);
    
    // Blend fill and outline with proper AA
    vec4 fill = vec4(color.rgb, color.a * fillAlpha);
    vec4 outline = vec4(outCol.rgb, outCol.a * outlineAlpha);
    
    // If inside, use fill (with outline showing through at edges)
    // If outside, use outline only
    if (distance < 0.0) {
        // Inside: blend fill with outline at the edge
        gl_FragColor = mix(outline, fill, fillAlpha);
    } else {
        // Outside: outline only
        gl_FragColor = outline;
    }
}

