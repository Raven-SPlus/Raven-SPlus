#ifdef GL_ES
precision mediump float;
#endif

#extension GL_OES_standard_derivatives : enable

uniform vec2 resolution;
uniform float time;

/*
* Astral shader
* Aurora-inspired nebula with lightweight starfield layers.
* Designed for richer visuals while staying performance-friendly.
*/

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * amp;
        p = p * 2.03 + vec2(17.0, 11.0);
        amp *= 0.5;
    }
    return value;
}

float starLayer(vec2 uv, float seed) {
    vec2 cell = floor(uv);
    vec2 local = fract(uv) - 0.5;

    float h = hash21(cell + seed);
    float star = step(0.993, h);

    vec2 jitter = vec2(hash21(cell + seed + 1.7), hash21(cell + seed + 4.2)) - 0.5;
    float d = length(local - jitter * 0.4);
    float glow = exp(-45.0 * d * d);

    return star * glow;
}

void main() {
    vec2 uv = gl_FragCoord.xy / resolution.xy;
    vec2 p = (gl_FragCoord.xy * 2.0 - resolution.xy) / resolution.y;
    float t = time;

    mat2 rot = mat2(cos(0.15), -sin(0.15), sin(0.15), cos(0.15));
    vec2 q = rot * p;

    float n1 = fbm(q * 1.8 + vec2(t * 0.05, -t * 0.03));
    float n2 = fbm(q * 3.2 + vec2(-t * 0.03, t * 0.06));
    float drift = n1 * 0.65 + n2 * 0.35;

    float bandA = sin(q.x * 2.2 + drift * 5.0 - t * 0.9);
    float bandB = sin(q.x * 3.4 - drift * 4.2 + t * 0.7);
    float aurora = smoothstep(-0.35, 0.85, bandA * 0.65 + bandB * 0.35);

    float horizon = smoothstep(1.15, -0.25, q.y);
    float nebulaMask = clamp(aurora * horizon + drift * 0.35, 0.0, 1.0);

    vec3 skyBottom = vec3(0.02, 0.03, 0.07);
    vec3 skyTop = vec3(0.03, 0.07, 0.15);
    vec3 baseSky = mix(skyBottom, skyTop, smoothstep(-1.0, 0.8, q.y));

    vec3 nebulaA = vec3(0.22, 0.42, 0.92);
    vec3 nebulaB = vec3(0.55, 0.24, 0.85);
    vec3 nebulaC = vec3(0.18, 0.78, 0.95);
    vec3 nebulaColor = mix(nebulaA, nebulaB, smoothstep(0.2, 0.85, drift));
    nebulaColor = mix(nebulaColor, nebulaC, aurora * 0.45);

    float starsFar = starLayer((uv + vec2(t * 0.003, 0.0)) * 150.0, 1.0);
    float starsNear = starLayer((uv + vec2(-t * 0.006, t * 0.002)) * 90.0, 19.0);
    float stars = starsFar * 0.85 + starsNear * 1.25;

    vec3 color = baseSky;
    color += nebulaColor * nebulaMask * 0.9;
    color += vec3(0.65, 0.8, 1.0) * stars;

    float centerGlow = exp(-2.2 * dot(p, p));
    color += vec3(0.06, 0.09, 0.16) * centerGlow;

    float vignette = 1.0 - smoothstep(0.7, 1.5, length(p));
    color *= 0.75 + vignette * 0.25;

    color = pow(clamp(color, 0.0, 1.0), vec3(0.96));
    gl_FragColor = vec4(color, 1.0);
}

