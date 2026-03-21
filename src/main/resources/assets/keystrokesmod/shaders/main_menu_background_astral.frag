#ifdef GL_ES
precision mediump float;
#endif

#extension GL_OES_standard_derivatives : enable

uniform vec2 resolution;
uniform float time;

/*
 * Astral Shader - Volumetric aurora with starfield and nebula
 *
 * Improvements over Aurora:
 *   - Volumetric light accumulation (no per-step normal computation)
 *   - Cosine palettes for richer color gradients
 *   - Dual-layer star field with parallax and twinkle
 *   - Subtle nebula background glow
 *   - Kaleidoscopic folding in the distance field
 *   - ACES filmic tonemapping
 *
 * Net performance is comparable: removing calcNormal (6 extra map()
 * calls per step in the original) frees budget for more march steps
 * and richer compositing at similar total cost.
 */

vec3 palette(float t, vec3 a, vec3 b, vec3 c, vec3 d) {
    return a + b * cos(6.28318 * (c * t + d));
}

mat2 rot(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, -s, s, c);
}

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float fbm3(vec3 p) {
    float f = 0.0;
    f += 0.500 * sin(p.x + sin(p.z * 0.7 + sin(p.y * 1.3)));
    p  = p.zxy * 1.7 + time * 0.12;
    f += 0.250 * sin(p.x + sin(p.z * 0.7 + sin(p.y * 1.3)));
    p  = p.zxy * 1.7 + time * 0.12;
    f += 0.125 * sin(p.x + sin(p.z * 0.7 + sin(p.y * 1.3)));
    return f;
}

float map(vec3 p) {
    p.xz *= rot(time * 0.22);
    p.xy *= rot(time * 0.13);

    vec3 q = abs(p) - 1.5;
    q = abs(q) - 0.7;

    float d = length(q) - 0.8;
    d += fbm3(p * 1.5) * 0.55;

    float d2 = length(p - vec3(sin(time * 0.35) * 0.5,
                                cos(time * 0.28) * 0.4, 0.0)) - 1.1;
    d2 += fbm3(p * 2.0 + 3.0) * 0.4;

    return min(d, d2) * 0.7;
}

vec3 stars(vec2 uv) {
    vec3 col = vec3(0.0);
    for (int layer = 0; layer < 2; layer++) {
        float scale = 60.0 + float(layer) * 40.0;
        vec2 id = floor(uv * scale);
        float h = hash21(id + float(layer) * 100.0);
        vec2 center = (id + 0.5) / scale;
        float d = length(uv - center) * scale;

        float brightness = smoothstep(0.6, 0.0, d)
                         * step(0.93 - float(layer) * 0.02, h);
        float twinkle = 0.7 + 0.3 * sin(time * (1.5 + h * 3.0) + h * 80.0);

        vec3 tint = mix(vec3(0.8, 0.85, 1.0), vec3(1.0, 0.9, 0.7), h);
        col += tint * brightness * twinkle;
    }
    return col;
}

void main() {
    vec2 uv = gl_FragCoord.xy / resolution.xy;
    float aspect = resolution.x / resolution.y;
    vec2 a = uv * 2.0 - 1.0;
    a.x *= aspect;

    vec3 rd = normalize(vec3(a, -1.2));
    vec3 ro = vec3(0.0, 0.0, 5.0);

    vec3 pa = vec3(0.5);
    vec3 pb = vec3(0.5);
    vec3 pc = vec3(1.0);
    vec3 pdCool = vec3(0.00, 0.10, 0.20);
    vec3 pdWarm = vec3(0.30, 0.20, 0.20);

    vec3 col = vec3(0.0);
    float t = 2.0;
    float absorption = 0.0;

    for (int i = 0; i < 12; i++) {
        vec3 p = ro + rd * t;
        float d = map(p);

        float density = smoothstep(1.5, -0.5, d);

        if (density > 0.01) {
            float ci = t * 0.07 + p.y * 0.12 + time * 0.055;

            vec3 c1 = palette(ci,             pa, pb, pc, pdCool);
            vec3 c2 = palette(ci * 0.7 + 0.5, pa, pb, pc, pdWarm);
            vec3 localColor = mix(c1, c2, sin(ci * 3.14) * 0.5 + 0.5);

            float fogFade = exp(-t * 0.07);
            float glow    = exp(-abs(d) * 3.0);
            float core    = exp(-abs(d) * 8.0) * 0.4;

            float alpha = density * (1.0 - absorption) * fogFade;
            col += localColor * alpha * (0.35 + glow * 0.45 + core * 0.2);
            absorption += alpha * 0.3;
        }

        t += max(abs(d) * 0.55, 0.18);
        if (t > 25.0 || absorption > 0.95) break;
    }

    // --- background ---
    float dist = length(a);

    vec3 bgTop = vec3(0.020, 0.012, 0.055);
    vec3 bgBot = vec3(0.008, 0.018, 0.035);
    vec3 bg = mix(bgBot, bgTop, uv.y * 0.8 + 0.1);

    float neb = fbm3(vec3(a * 2.5, time * 0.04)) * 0.5 + 0.5;
    bg += palette(neb * 0.5 + time * 0.02, pa, pb, pc, pdCool) * neb * 0.035;

    bg += stars(uv);

    col = bg * (1.0 - absorption * 0.8) + col;

    // vignette
    float vignette = 1.0 - smoothstep(0.5, 1.4, dist * 0.7);
    col *= 0.7 + 0.3 * vignette;

    // ACES filmic tonemapping
    col = col * (2.51 * col + 0.03) / (col * (2.43 * col + 0.59) + 0.14);

    col = pow(col, vec3(0.93));
    col = clamp(col, 0.0, 1.0);

    gl_FragColor = vec4(col, 1.0);
}
